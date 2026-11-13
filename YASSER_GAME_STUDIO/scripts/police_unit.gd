extends CharacterBody3D

var target: Node3D
var pursuit_speed: float = 8.0
var health: float = 180.0
var attack_cooldown: float = 0.0
var gravity: float = 9.8

func configure(follow_target: Node3D, speed: float) -> void:
	target = follow_target
	pursuit_speed = speed

func _ready() -> void:
	add_to_group("police")
	collision_layer = 1
	collision_mask = 1

	var collision := CollisionShape3D.new()
	var shape := BoxShape3D.new()
	shape.size = Vector3(1.8, 1.1, 3.4)
	collision.shape = shape
	collision.position.y = 0.62
	add_child(collision)

	var body := MeshInstance3D.new()
	var body_mesh := BoxMesh.new()
	body_mesh.size = Vector3(1.8, 0.85, 3.4)
	body.mesh = body_mesh
	body.position.y = 0.62
	var body_material := StandardMaterial3D.new()
	body_material.albedo_color = Color(0.10, 0.14, 0.20)
	body.material_override = body_material
	add_child(body)

	_add_beacon(Vector3(-0.42, 1.18, 0.0), Color(0.08, 0.28, 1.0))
	_add_beacon(Vector3(0.42, 1.18, 0.0), Color(1.0, 0.08, 0.08))

func _physics_process(delta: float) -> void:
	attack_cooldown = maxf(0.0, attack_cooldown - delta)
	if not is_instance_valid(target):
		queue_free()
		return

	var offset := target.global_position - global_position
	offset.y = 0.0
	var distance := offset.length()
	if distance > 0.1:
		var direction := _avoid_obstacle(offset.normalized())
		velocity.x = direction.x * pursuit_speed
		velocity.z = direction.z * pursuit_speed
		look_at(global_position + direction, Vector3.UP)
	else:
		velocity.x = 0.0
		velocity.z = 0.0

	if not is_on_floor():
		velocity.y -= gravity * delta
	else:
		velocity.y = 0.0
	move_and_slide()

	if distance < 3.0 and attack_cooldown <= 0.0:
		attack_cooldown = 1.4
		if target.has_method("apply_damage"):
			target.call("apply_damage", 7.0)

func take_damage(amount: float) -> void:
	health -= amount
	if health <= 0.0:
		queue_free()

func _add_beacon(beacon_position: Vector3, color: Color) -> void:
	var beacon := MeshInstance3D.new()
	var mesh := BoxMesh.new()
	mesh.size = Vector3(0.42, 0.16, 0.3)
	beacon.mesh = mesh
	beacon.position = beacon_position
	var material := StandardMaterial3D.new()
	material.albedo_color = color
	material.emission_enabled = true
	material.emission = color
	beacon.material_override = material
	add_child(beacon)

func _avoid_obstacle(desired_direction: Vector3) -> Vector3:
	var space := get_world_3d().direct_space_state
	var origin := global_position + Vector3.UP * 0.8
	var forward_query := PhysicsRayQueryParameters3D.create(
		origin,
		origin + desired_direction * 5.0
	)
	forward_query.exclude = [get_rid()]
	forward_query.collision_mask = collision_mask
	var forward_hit := space.intersect_ray(forward_query)
	if forward_hit.is_empty() or forward_hit.get("collider") == target:
		return desired_direction

	var side := Vector3.UP.cross(desired_direction).normalized()
	var left_query := PhysicsRayQueryParameters3D.create(origin, origin - side * 3.0)
	left_query.exclude = [get_rid()]
	left_query.collision_mask = collision_mask
	var right_query := PhysicsRayQueryParameters3D.create(origin, origin + side * 3.0)
	right_query.exclude = [get_rid()]
	right_query.collision_mask = collision_mask
	var left_blocked := not space.intersect_ray(left_query).is_empty()
	var right_blocked := not space.intersect_ray(right_query).is_empty()

	if left_blocked and not right_blocked:
		return (desired_direction + side * 0.9).normalized()
	if right_blocked and not left_blocked:
		return (desired_direction - side * 0.9).normalized()
	if not left_blocked and not right_blocked:
		return (desired_direction + side * 0.65).normalized()
	return desired_direction
