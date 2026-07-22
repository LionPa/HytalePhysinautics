use crate::PhysicsWorld;
use rapier3d::prelude::SharedShape;
use rapier3d::math::{Vector, Pose};
use rapier3d::geometry::ColliderBuilder;

#[unsafe(no_mangle)]
pub unsafe extern "C" fn check_shapes_batch(
    world_ptr: *mut PhysicsWorld,
    shape_ids_ptr: *const u32,
    count: u32,
    out_bitmask_ptr: *mut u8
) {
    if world_ptr.is_null() || count == 0 { return; }
    let world = unsafe { &*world_ptr };
    let shape_ids = unsafe { std::slice::from_raw_parts(shape_ids_ptr, count as usize) };
    let out_bitmask = unsafe { std::slice::from_raw_parts_mut(out_bitmask_ptr, ((count + 7) / 8) as usize) };

    // Очищаем битовую маску перед записью
    for b in out_bitmask.iter_mut() {
        *b = 0;
    }

    for (i, &shape_id) in shape_ids.iter().enumerate() {
        if world.shapes.contains_key(&shape_id) {
            let byte_idx = i / 8;
            let bit_idx = i % 8;
            out_bitmask[byte_idx] |= 1 << bit_idx;
        }
    }
}

#[unsafe(no_mangle)]
pub unsafe extern "C" fn add_shape(
    world_ptr: *mut PhysicsWorld,
    shape_id: u32,
    boxes_count: u32,
    boxes_ptr: *const f32
) {
    if world_ptr.is_null() || boxes_count == 0 { return; }
    let world = unsafe { &mut *world_ptr };
    let boxes = unsafe { std::slice::from_raw_parts(boxes_ptr, (boxes_count * 6) as usize) };

    if boxes_count == 1 {
        let min_x = boxes[0];
        let min_y = boxes[1];
        let min_z = boxes[2];
        let max_x = boxes[3];
        let max_y = boxes[4];
        let max_z = boxes[5];

        let half_extents = Vector::new(
            (max_x - min_x) / 2.0,
            (max_y - min_y) / 2.0,
            (max_z - min_z) / 2.0
        );

        let center = Vector::new(
            (min_x + max_x) / 2.0,
            (min_y + max_y) / 2.0,
            (min_z + max_z) / 2.0
        );

        // В Hytale сложные блоки часто смещены. 
        // Мы используем CompoundShape даже для 1 бокса, чтобы применить translation.
        let shape = SharedShape::cuboid(half_extents.x, half_extents.y, half_extents.z);
        let iso = Pose::from_translation(center);
        
        world.shapes.insert(shape_id, vec![(iso, shape)]);
    } else {
        let mut parts = Vec::with_capacity(boxes_count as usize);

        for i in 0..boxes_count as usize {
            let min_x = boxes[i * 6];
            let min_y = boxes[i * 6 + 1];
            let min_z = boxes[i * 6 + 2];
            let max_x = boxes[i * 6 + 3];
            let max_y = boxes[i * 6 + 4];
            let max_z = boxes[i * 6 + 5];

            let half_extents = Vector::new(
                (max_x - min_x) / 2.0,
                (max_y - min_y) / 2.0,
                (max_z - min_z) / 2.0
            );

            let center = Vector::new(
                (min_x + max_x) / 2.0,
                (min_y + max_y) / 2.0,
                (min_z + max_z) / 2.0
            );

            let shape = SharedShape::cuboid(half_extents.x, half_extents.y, half_extents.z);
            let iso = Pose::from_translation(center);
            parts.push((iso, shape));
        }

        world.shapes.insert(shape_id, parts);
    }
}
