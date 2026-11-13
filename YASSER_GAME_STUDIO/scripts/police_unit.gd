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
		var direction := offset.normalized()
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
