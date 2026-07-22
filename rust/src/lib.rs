mod level_collider;
use std::collections::HashMap;

use rapier3d::prelude::*;
use rapier3d::math::Vector;
use std::ptr;

pub struct PhysicsWorld {
    pub gravity: Vector,
    pub integration_parameters: IntegrationParameters,
    pub physics_pipeline: PhysicsPipeline,
    pub island_manager: IslandManager,
    pub broad_phase: BroadPhaseBvh,
    pub narrow_phase: NarrowPhase,
    pub rigid_body_set: RigidBodySet,
    pub collider_set: ColliderSet,
    pub impulse_joint_set: ImpulseJointSet,
    pub multibody_joint_set: MultibodyJointSet,
    pub ccd_solver: CCDSolver,

    pub handles: Vec<Option<RigidBodyHandle>>,
    pub free_ids: Vec<u32>,

    pub sync_buffer: *mut f32,
    pub max_objects: usize,

    pub chunk_colliders: HashMap<(i32, i32, i32), Option<ColliderHandle>>,
}

#[unsafe(no_mangle)]
pub unsafe extern "C" fn rapier_init() -> *mut PhysicsWorld {
    let rigid_body_set = RigidBodySet::new();
    let collider_set = ColliderSet::new();

    let world = Box::new(PhysicsWorld {
        gravity: Vector::new(0.0, -9.81, 0.0),
        integration_parameters: IntegrationParameters::default(),
        physics_pipeline: PhysicsPipeline::new(),
        island_manager: IslandManager::new(),
        broad_phase: BroadPhaseBvh::new(),
        narrow_phase: NarrowPhase::new(),
        rigid_body_set,
        collider_set,
        impulse_joint_set: ImpulseJointSet::new(),
        multibody_joint_set: MultibodyJointSet::new(),
        ccd_solver: CCDSolver::new(),
        handles: Vec::new(),
        free_ids: Vec::new(),
        sync_buffer: ptr::null_mut(),
        max_objects: 0,
        chunk_colliders: HashMap::new(),
    });

    Box::into_raw(world)
}

#[unsafe(no_mangle)]
pub unsafe extern "C" fn rapier_set_sync_buffer(world_ptr: *mut PhysicsWorld, ptr: *mut f32, max_objects: u32) {
    if world_ptr.is_null() { return; }
    let world = unsafe { &mut *world_ptr };
    world.sync_buffer = ptr;
    world.max_objects = max_objects as usize;
}

#[unsafe(no_mangle)]
pub unsafe extern "C" fn rapier_add_box(world_ptr: *mut PhysicsWorld, x: f32, y: f32, z: f32, size: f32) -> u32 {
    if world_ptr.is_null() { return 0; }
    let world = unsafe { &mut *world_ptr };

    let rigid_body = RigidBodyBuilder::dynamic()
        .translation(Vector::new(x, y, z))
        .build();
        
    let handle = world.rigid_body_set.insert(rigid_body);

    let half_extents = size / 2.0;
    let collider = ColliderBuilder::cuboid(half_extents, half_extents, half_extents)
        .restitution(0.3)
        .friction(0.5)
        .build();
        
    world.collider_set.insert_with_parent(collider, handle, &mut world.rigid_body_set);

    let id = if let Some(free_id) = world.free_ids.pop() {
        world.handles[free_id as usize] = Some(handle);
        free_id
    } else {
        let new_id = world.handles.len() as u32;
        world.handles.push(Some(handle));
        new_id
    };

    if !world.sync_buffer.is_null() && (id as usize) < world.max_objects {
        let offset = (id as usize) * 7;
        let buffer = unsafe { std::slice::from_raw_parts_mut(world.sync_buffer, world.max_objects * 7) };
        buffer[offset]     = x;
        buffer[offset + 1] = y;
        buffer[offset + 2] = z;
        buffer[offset + 3] = 0.0;
        buffer[offset + 4] = 0.0;
        buffer[offset + 5] = 0.0;
        buffer[offset + 6] = 1.0;
    }
    
    id
}

#[unsafe(no_mangle)]
pub unsafe extern "C" fn rapier_apply_impulse(world_ptr: *mut PhysicsWorld, id: u32, fx: f32, fy: f32, fz: f32) {
    if world_ptr.is_null() { return; }
    let world = unsafe { &mut *world_ptr };

    if let Some(&Some(handle)) = world.handles.get(id as usize) {
        if let Some(rb) = world.rigid_body_set.get_mut(handle) {
            rb.apply_impulse(Vector::new(fx, fy, fz), true);
        }
    }
}

#[unsafe(no_mangle)]
pub unsafe extern "C" fn rapier_step(world_ptr: *mut PhysicsWorld, dt: f32) {
    if world_ptr.is_null() { return; }
    let world = unsafe { &mut *world_ptr };

    world.integration_parameters.dt = dt;

    world.physics_pipeline.step(
        world.gravity,
        &world.integration_parameters,
        &mut world.island_manager,
        &mut world.broad_phase,
        &mut world.narrow_phase,
        &mut world.rigid_body_set,
        &mut world.collider_set,
        &mut world.impulse_joint_set,
        &mut world.multibody_joint_set,
        &mut world.ccd_solver,
        &(),
        &(),
    );

    if !world.sync_buffer.is_null() && world.max_objects > 0 {
        let buffer = unsafe { std::slice::from_raw_parts_mut(world.sync_buffer, world.max_objects * 7) };
        
        for (i, opt_handle) in world.handles.iter().enumerate() {
            if i >= world.max_objects { break; }
            if let Some(handle) = opt_handle {
                if let Some(rb) = world.rigid_body_set.get(*handle) {
                    let pos = rb.translation();
                    let rot = rb.rotation(); 
                    
                    let offset = i * 7;
                    buffer[offset]     = pos.x;
                    buffer[offset + 1] = pos.y;
                    buffer[offset + 2] = pos.z;

                    buffer[offset + 3] = rot.x;
                    buffer[offset + 4] = rot.y;
                    buffer[offset + 5] = rot.z;
                    buffer[offset + 6] = rot.w;
                }
            }
        }
    }
}

#[unsafe(no_mangle)]
pub unsafe extern "C" fn rapier_remove_body(world_ptr: *mut PhysicsWorld, id: u32) {
    if world_ptr.is_null() { return; }
    let world = unsafe { &mut *world_ptr };

    if let Some(opt_handle) = world.handles.get_mut(id as usize) {
        if let Some(handle) = opt_handle.take() {
            world.rigid_body_set.remove(
                handle,
                &mut world.island_manager,
                &mut world.collider_set,
                &mut world.impulse_joint_set,
                &mut world.multibody_joint_set,
                true,
            );

            if id as usize == world.handles.len() - 1 {
                while let Some(None) = world.handles.last() {
                    world.handles.pop();
                }
                world.free_ids.retain(|&free_id| (free_id as usize) < world.handles.len());
            } else {
                world.free_ids.push(id);
            }
        }
    }
}
