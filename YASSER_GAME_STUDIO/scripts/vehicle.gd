extends VehicleBody3D

@export var max_engine_force: float = 1450.0
@export var max_brake_force: float = 38.0
@export var max_steering_angle: float = 0.48

var driver_active: bool = false

func set_driver_active(active: bool) -> void:
	driver_active = active
	if not active:
		engine_force = 0.0
		brake = 8.0

func _physics_process(delta: float) -> void:
	if not driver_active:
		engine_force = 0.0
		steering = move_toward(steering, 0.0, delta * 1.8)
		brake = 5.0
		return

	var throttle := Input.get_action_strength("vehicle_forward") - Input.get_action_strength("vehicle_reverse")
	var turn := Input.get_action_strength("vehicle_right") - Input.get_action_strength("vehicle_left")
	var brake_input := Input.get_action_strength("vehicle_brake")

	engine_force = throttle * max_engine_force
	brake = brake_input * max_brake_force
	steering = move_toward(steering, turn * max_steering_angle, delta * 2.4)
