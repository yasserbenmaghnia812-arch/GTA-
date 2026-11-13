extends CharacterBody3D

const WEAPON_SCRIPT = preload("res://scripts/weapon_system.gd")

signal weapon_status_changed(weapon_name: String, magazine: int, reserve: int)
signal health_changed(health: int)
signal crime_reported(position: Vector3)
signal vehicle_tuning_changed(label: String)

@export var walk_speed: float = 6.0
@export var run_speed: float = 9.0
@export var jump_velocity: float = 4.8
@export var look_sensitivity: float = 0.0035

@onready var camera_pivot: Node3D = $CameraPivot
@onready var camera: Camera3D = $CameraPivot/Camera3D

var current_vehicle: VehicleBody3D = null
var camera_pitch: float = -0.22
var camera_yaw: float = 0.0
var weapon_system
var health: float = 100.0
var respawn_timer: float = 0.0

func _ready() -> void:
	camera_pitch = camera_pivot.rotation.x
	camera_yaw = rotation.y
	if not OS.has_feature("mobile"):
		Input.mouse_mode = Input.MOUSE_MODE_CAPTURED
	weapon_system = WEAPON_SCRIPT.new()
	weapon_system.status_changed.connect(_on_weapon_status_changed)
	weapon_system.crime_committed.connect(_on_crime_committed)
	add_child(weapon_system)
	weapon_system.publish_status()
	health_changed.emit(int(health))

func _input(event: InputEvent) -> void:
	if event is InputEventMouseMotion and Input.mouse_mode == Input.MOUSE_MODE_CAPTURED:
		_look(event.relative)
	elif event is InputEventScreenDrag:
		var viewport_size: Vector2 = get_viewport().get_visible_rect().size
		var screen_width: float = viewport_size.x
		if event.position.x > screen_width * 0.45 and event.position.y < viewport_size.y - 210.0:
			_look(event.relative)
	elif event is InputEventKey and event.pressed and event.keycode == KEY_ESCAPE:
		Input.mouse_mode = Input.MOUSE_MODE_VISIBLE

func _look(delta: Vector2) -> void:
	camera_yaw -= delta.x * look_sensitivity
	camera_pitch = clampf(camera_pitch - delta.y * look_sensitivity, -0.95, 0.35)
	rotation.y = camera_yaw
	camera_pivot.rotation.x = camera_pitch

func _physics_process(delta: float) -> void:
	if health <= 0.0:
		respawn_timer = maxf(0.0, respawn_timer - delta)
		if respawn_timer <= 0.0:
			_respawn()
		return

	if weapon_system != null:
		if Input.is_action_just_pressed("weapon_next"):
			weapon_system.next_weapon()
		if Input.is_action_just_pressed("reload"):
			weapon_system.reload()
		if Input.is_action_pressed("fire") and current_vehicle == null:
			weapon_system.try_fire(camera, self)

	if Input.is_action_just_pressed("interact"):
		_toggle_vehicle()
	if Input.is_action_just_pressed("vehicle_tune") and current_vehicle != null:
		var setup_label: String = str(current_vehicle.call("cycle_tuning"))
		vehicle_tuning_changed.emit(setup_label)

	if current_vehicle != null:
		global_position = current_vehicle.global_position + Vector3(0, 1.1, 0)
		rotation.y = current_vehicle.rotation.y
		velocity = Vector3.ZERO
		return

	var input_dir := Input.get_vector("move_left", "move_right", "move_forward", "move_back")
	var camera_forward := -camera_pivot.global_transform.basis.z
	camera_forward.y = 0.0
	camera_forward = camera_forward.normalized()
	var camera_right := camera_pivot.global_transform.basis.x
	camera_right.y = 0.0
	camera_right = camera_right.normalized()
	var direction := (camera_right * input_dir.x + camera_forward * -input_dir.y).normalized()

	var target_speed := run_speed if Input.is_action_pressed("sprint") else walk_speed
	if direction.length_squared() > 0.001:
		velocity.x = direction.x * target_speed
		velocity.z = direction.z * target_speed
	else:
		velocity.x = move_toward(velocity.x, 0.0, walk_speed * 7.0 * delta)
		velocity.z = move_toward(velocity.z, 0.0, walk_speed * 7.0 * delta)

	if not is_on_floor():
		velocity.y -= 9.8 * delta
	elif Input.is_action_just_pressed("jump"):
		velocity.y = jump_velocity

	move_and_slide()

func _toggle_vehicle() -> void:
	if current_vehicle != null:
		var vehicle := current_vehicle
		vehicle.call("set_driver_active", false)
		current_vehicle = null
		visible = true
		collision_layer = 1
		collision_mask = 1
		global_position = vehicle.global_position + vehicle.global_transform.basis.x * 2.0 + Vector3(0, 1.0, 0)
		return

	var nearest: VehicleBody3D = null
	var nearest_distance := 5.0
	for candidate in get_tree().get_nodes_in_group("vehicles"):
		if candidate is VehicleBody3D:
			var distance := global_position.distance_to(candidate.global_position)
			if distance < nearest_distance:
				nearest_distance = distance
				nearest = candidate
	if nearest != null:
		current_vehicle = nearest
		current_vehicle.call("set_driver_active", true)
		visible = false
		collision_layer = 0
		collision_mask = 0

func apply_damage(amount: float) -> void:
	health = maxf(0.0, health - amount)
	health_changed.emit(int(health))
	if health <= 0.0:
		respawn_timer = 3.0
		if current_vehicle != null:
			current_vehicle.call("set_driver_active", false)
			current_vehicle = null
		visible = true
		collision_layer = 1
		collision_mask = 1

func _respawn() -> void:
	health = 100.0
	velocity = Vector3.ZERO
	global_position = Vector3(0, 1.0, 9.0)
	health_changed.emit(int(health))

func _on_weapon_status_changed(weapon_name: String, magazine: int, reserve: int) -> void:
	weapon_status_changed.emit(weapon_name, magazine, reserve)

func _on_crime_committed(position: Vector3) -> void:
	crime_reported.emit(position)
