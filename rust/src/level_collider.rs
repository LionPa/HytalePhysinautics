use rapier3d::geometry::ColliderBuilder;
use rapier3d::math::{IVector, Vector};
use rapier3d::prelude::SharedShape;
use crate::PhysicsWorld;

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

    // Вычисляем мировые координаты чанка прямо здесь (x * 32)
    let world_x = (chunk_x as f32) * 32.0;
    let world_y = (chunk_y as f32) * 32.0;
    let world_z = (chunk_z as f32) * 32.0;

    let collider = ColliderBuilder::voxels(Vector::new(1.0, 1.0, 1.0), voxels_slice)
        .translation(Vector::new(world_x, world_y, world_z))
        .friction(0.8)
        .build();

    let handle = world.collider_set.insert(collider);
    world.chunk_colliders.insert((chunk_x, chunk_y, chunk_z), Some(handle));
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

    // Распаковываем 4 КБ битов
    for index in 0..32768 {
        let byte_index = index / 8;
        let bit_index = index % 8;
        
        // 1 - блок, 0 - воздух
        if (bitmask[byte_index] & (1 << bit_index)) != 0 {
            let x = (index % 32) as i32;
            let y = ((index / 32) % 32) as i32;
            let z = (index / 1024) as i32;
            
            voxels.push(IVector::new(x, y, z));
        }
    }

    // Если чанк полностью состоит из воздуха
    if voxels.is_empty() {
        // Удаляем старый коллайдер, если он был
        if let Some(Some(handle)) = world.chunk_colliders.remove(&(chunk_x, chunk_y, chunk_z)) {
            world.collider_set.remove(handle, &mut world.island_manager, &mut world.rigid_body_set, true);
        }
        // Записываем None, чтобы Kotlin знал, что чанк обработан (он пустой)
        world.chunk_colliders.insert((chunk_x, chunk_y, chunk_z), None);
        return;
    }

    let new_shape = SharedShape::voxels(Vector::new(1.0, 1.0, 1.0), &voxels);

    if let Some(opt_handle) = world.chunk_colliders.get_mut(&(chunk_x, chunk_y, chunk_z)) {
        if let Some(handle) = opt_handle {
            if let Some(collider) = world.collider_set.get_mut(*handle) {
                collider.set_shape(new_shape);
            }
        } else {
            // Был None (пустой), стал заполненным
            let world_x = (chunk_x as f32) * 32.0;
            let world_y = (chunk_y as f32) * 32.0;
            let world_z = (chunk_z as f32) * 32.0;

            let collider = ColliderBuilder::new(new_shape)
                .translation(Vector::new(world_x, world_y, world_z))
                .friction(0.8)
                .build();

            *opt_handle = Some(world.collider_set.insert(collider));
        }
    } else {
        // Вообще не было в мапе
        let world_x = (chunk_x as f32) * 32.0;
        let world_y = (chunk_y as f32) * 32.0;
        let world_z = (chunk_z as f32) * 32.0;

        let collider = ColliderBuilder::new(new_shape)
            .translation(Vector::new(world_x, world_y, world_z))
            .friction(0.8)
            .build();

        let handle = world.collider_set.insert(collider);
        world.chunk_colliders.insert((chunk_x, chunk_y, chunk_z), Some(handle));
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
    
    if let Some(Some(handle)) = world.chunk_colliders.remove(&(chunk_x, chunk_y, chunk_z)) {
        world.collider_set.remove(
            handle,
            &mut world.island_manager,
            &mut world.rigid_body_set,
            true
        );
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