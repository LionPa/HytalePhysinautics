use rapier3d::geometry::ColliderBuilder;
use rapier3d::math::{IVector, Vector, Pose};
use rapier3d::prelude::SharedShape;
use crate::PhysicsWorld;

#[repr(u32)]
pub enum PhysicsMaterial {
    Default = 0,
    Slippery = 1,
    Sticky = 2,
    Bouncy = 3,
}

impl PhysicsMaterial {
    pub fn from_u32(value: u32) -> Self {
        match value {
            1 => PhysicsMaterial::Slippery,
            2 => PhysicsMaterial::Sticky,
            3 => PhysicsMaterial::Bouncy,
            _ => PhysicsMaterial::Default,
        }
    }

    pub fn properties(&self) -> (f32, f32) {
        match self {
            PhysicsMaterial::Default => (0.8, 0.0),
            PhysicsMaterial::Slippery => (0.1, 0.0),
            PhysicsMaterial::Sticky => (1.5, 0.0),
            PhysicsMaterial::Bouncy => (0.5, 0.8),
        }
    }
}

#[unsafe(no_mangle)]
pub unsafe extern "C" fn add_chunk(
    world_ptr: *mut PhysicsWorld,
    chunk_x: i32,
    chunk_y: i32,
    chunk_z: i32,
    flat_coords_ptr: *const i32,
    blocks_count: u32
) {
    if world_ptr.is_null() || blocks_count == 0 { return; }
    let world = unsafe { &mut *world_ptr };

    let voxels_slice: &[IVector] = unsafe {
        std::slice::from_raw_parts(
            flat_coords_ptr as *const IVector,
            blocks_count as usize
        )
    };

    let world_x = (chunk_x as f32) * 32.0;
    let world_y = (chunk_y as f32) * 32.0;
    let world_z = (chunk_z as f32) * 32.0;

    let collider = ColliderBuilder::voxels(Vector::new(1.0, 1.0, 1.0), voxels_slice)
        .translation(Vector::new(world_x, world_y, world_z))
        .friction(0.8)
        .build();

    let handle = world.collider_set.insert(collider);
    
    let mut chunk_map = std::collections::HashMap::new();
    chunk_map.insert(0, handle);
    world.chunk_colliders.insert((chunk_x, chunk_y, chunk_z), chunk_map);
}

#[unsafe(no_mangle)]
pub unsafe extern "C" fn set_chunk(
    world_ptr: *mut PhysicsWorld,
    chunk_x: i32,
    chunk_y: i32,
    chunk_z: i32,
    bitmask_ptr: *const u8
) {
    if world_ptr.is_null() { return; }
    let world = unsafe { &mut *world_ptr };

    let bitmask = unsafe { std::slice::from_raw_parts(bitmask_ptr, 4096) };
    let mut voxels = Vec::with_capacity(512);

    for byte_index in 0..4096 {
        let b = bitmask[byte_index];
        if b != 0 {
            for bit_index in 0..8 {
                if (b & (1 << bit_index)) != 0 {
                    let index = byte_index * 8 + bit_index;
                    let x = (index % 32) as i32;
                    let y = ((index / 32) % 32) as i32;
                    let z = (index / 1024) as i32;
                    voxels.push(IVector::new(x, y, z));
                }
            }
        }
    }

    let chunk_map = world.chunk_colliders.entry((chunk_x, chunk_y, chunk_z)).or_insert_with(std::collections::HashMap::new);

    if voxels.is_empty() {
        if let Some(handle) = chunk_map.remove(&0) {
            world.collider_set.remove(handle, &mut world.island_manager, &mut world.rigid_body_set, true);
        }
        return;
    }

    let new_shape = SharedShape::voxels(Vector::new(1.0, 1.0, 1.0), &voxels);

    if let Some(&handle) = chunk_map.get(&0) {
        if let Some(collider) = world.collider_set.get_mut(handle) {
            collider.set_shape(new_shape);
        }
    } else {
        let world_x = (chunk_x as f32) * 32.0;
        let world_y = (chunk_y as f32) * 32.0;
        let world_z = (chunk_z as f32) * 32.0;

        let collider = ColliderBuilder::new(new_shape)
            .translation(Vector::new(world_x, world_y, world_z))
            .friction(0.8)
            .build();

        let handle = world.collider_set.insert(collider);
        chunk_map.insert(0, handle);
    }
}

#[unsafe(no_mangle)]
pub unsafe extern "C" fn set_chunk_special(
    world_ptr: *mut PhysicsWorld,
    chunk_x: i32,
    chunk_y: i32,
    chunk_z: i32,
    shape_id: u32,
    material_type: u32,
    instances_count: u32,
    instances_ptr: *const u32
) {
    if world_ptr.is_null() { return; }
    let world = unsafe { &mut *world_ptr };

    let layer_key = (shape_id as u64) | ((material_type as u64) << 32);
    let chunk_map = world.chunk_colliders.entry((chunk_x, chunk_y, chunk_z)).or_insert_with(std::collections::HashMap::new);

    if instances_count == 0 {
        if let Some(handle) = chunk_map.remove(&layer_key) {
            world.collider_set.remove(handle, &mut world.island_manager, &mut world.rigid_body_set, true);
        }
        return;
    }

    let instances = unsafe { std::slice::from_raw_parts(instances_ptr, instances_count as usize) };
    let mut shapes = Vec::with_capacity(instances_count as usize);

    let default_parts = vec![(Pose::from_translation(Vector::new(0.5, 0.5, 0.5)), SharedShape::cuboid(0.5, 0.5, 0.5))];
    let base_parts = if let Some(parts) = world.shapes.get(&shape_id) {
        parts
    } else {
        &default_parts
    };

    for &packed in instances {
        let x = (packed & 0xFF) as f32;
        let y = ((packed >> 8) & 0xFF) as f32;
        let z = ((packed >> 16) & 0xFF) as f32;
        let rot_idx = ((packed >> 24) & 0xFF) as u8;

        let block_iso = Pose::from_translation(Vector::new(x, y, z));
        
        let center_iso = Pose::from_translation(Vector::new(0.5, 0.5, 0.5));
        let inv_center_iso = Pose::from_translation(Vector::new(-0.5, -0.5, -0.5));
        
        let yaw_idx = rot_idx % 4;
        let pitch_idx = (rot_idx / 4) % 4;
        let roll_idx = (rot_idx / 16) % 4;

        let yaw_angle = (yaw_idx as f32) * std::f32::consts::FRAC_PI_2;
        let pitch_angle = (pitch_idx as f32) * std::f32::consts::FRAC_PI_2;
        let roll_angle = (roll_idx as f32) * std::f32::consts::FRAC_PI_2;

        let yaw_iso = Pose::new(Vector::ZERO, Vector::new(0.0, yaw_angle, 0.0));
        let pitch_iso = Pose::new(Vector::ZERO, Vector::new(pitch_angle, 0.0, 0.0));
        let roll_iso = Pose::new(Vector::ZERO, Vector::new(0.0, 0.0, roll_angle));

        let rot_iso = yaw_iso * pitch_iso * roll_iso;
        
        let local_transform = center_iso * rot_iso * inv_center_iso;
        
        for (local_iso, part_shape) in base_parts {
            let combined_iso = block_iso * local_transform * local_iso;
            shapes.push((combined_iso, part_shape.clone()));
        }
    }

    let compound = SharedShape::compound(shapes);

    let material = PhysicsMaterial::from_u32(material_type);
    let (friction, restitution) = material.properties();

    if let Some(&handle) = chunk_map.get(&layer_key) {
        if let Some(collider) = world.collider_set.get_mut(handle) {
            collider.set_shape(compound);
            collider.set_friction(friction);
            collider.set_restitution(restitution);
        }
    } else {
        let world_x = (chunk_x as f32) * 32.0;
        let world_y = (chunk_y as f32) * 32.0;
        let world_z = (chunk_z as f32) * 32.0;

        let collider = ColliderBuilder::new(compound)
            .translation(Vector::new(world_x, world_y, world_z))
            .friction(friction)
            .restitution(restitution)
            .build();

        let handle = world.collider_set.insert(collider);
        chunk_map.insert(layer_key, handle);
    }
}

#[unsafe(no_mangle)]
pub unsafe extern "C" fn remove_chunk(
    world_ptr: *mut PhysicsWorld,
    chunk_x: i32,
    chunk_y: i32,
    chunk_z: i32
) {
    if world_ptr.is_null() { return; }
    let world = unsafe { &mut *world_ptr };
    
    if let Some(chunk_map) = world.chunk_colliders.remove(&(chunk_x, chunk_y, chunk_z)) {
        for (_, handle) in chunk_map {
            world.collider_set.remove(
                handle,
                &mut world.island_manager,
                &mut world.rigid_body_set,
                true
            );
        }
    }
}

#[unsafe(no_mangle)]
pub unsafe extern "C" fn has_chunk(
    world_ptr: *mut PhysicsWorld,
    chunk_x: i32,
    chunk_y: i32,
    chunk_z: i32
) -> bool {
    if world_ptr.is_null() { return false; }
    let world = unsafe { &*world_ptr };

    world.chunk_colliders.contains_key(&(chunk_x, chunk_y, chunk_z))
}