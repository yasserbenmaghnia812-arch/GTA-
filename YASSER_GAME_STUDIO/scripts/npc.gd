extends CharacterBody3D

@export var walk_speed: float = 1.15
@export var health: float = 80.0

var patrol_points: Array[Vector3] = []
var patrol_index: int = 0
var gravity: float = 9.8

func set_patrol_points(points: Array) -> void:
	patrol_points.clear()
	for point in points:
		if point is Vector3:
			patrol_points.append(point)
	if not patrol_points.is_empty():
		global_position = patrol_points[0]

func _ready() -> void:
	add_to_group("npcs")
	collision_layer = 1
	collision_mask = 1

	var collision := CollisionShape3D.new()
	var capsule_shape := CapsuleShape3D.new()
	capsule_shape.radius = 0.32
	capsule_shape.height = 1.55
	collision.shape = capsule_shape
	collision.position.y = 0.78
	add_child(collision)

	var visual := MeshInstance3D.new()
	var capsule_mesh := CapsuleMesh.new()
	capsule_mesh.radius = 0.32
	capsule_mesh.height = 1.55
	visual.mesh = capsule_mesh
	visual.position.y = 0.78
	var material := StandardMaterial3D.new()
	material.albedo_color = Color(0.58, 0.66, 0.72)
	visual.material_override = material
	add_child(visual)

func _physics_process(delta: float) -> void:
	if patrol_points.is_empty():
		return
	var destination := patrol_points[patrol_index]
	var offset := destination - global_position
	offset.y = 0.0
	if offset.length() < 1.0:
		patrol_index = (patrol_index + 1) % patrol_points.size()
		offset = patrol_points[patrol_index] - global_position
		offset.y = 0.0

	if offset.length_squared() > 0.01:
		var direction := offset.normalized()
		velocity.x = direction.x * walk_speed
		velocity.z = direction.z * walk_speed
		rotation.y = atan2(-direction.x, -direction.z)
	else:
		velocity.x = move_toward(velocity.x, 0.0, walk_speed * delta)
		velocity.z = move_toward(velocity.z, 0.0, walk_speed * delta)

	if not is_on_floor():
		velocity.y -= gravity * delta
	else:
		velocity.y = 0.0
	move_and_slide()

func take_damage(amount: float) -> void:
	health -= amount
	if health <= 0.0:
		queue_free()
