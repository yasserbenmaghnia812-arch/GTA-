extends VehicleBody3D

@export var vehicle_label: String = "COUPE"
@export var paint_color: Color = Color(0.16, 0.42, 0.78)
@export var max_engine_force: float = 1450.0
@export var max_brake_force: float = 38.0
@export var max_steering_angle: float = 0.48

var driver_active: bool = false
var tuning_index: int = 0
const TUNING_PRESETS: Array[Dictionary] = [
	{"name": "BALANCED", "engine": 1450.0, "steering": 0.46, "brake": 38.0, "color": Color(0.16, 0.42, 0.78)},
	{"name": "SPORT", "engine": 2050.0, "steering": 0.58, "brake": 48.0, "color": Color(0.86, 0.20, 0.12)},
	{"name": "GRIP", "engine": 1650.0, "steering": 0.40, "brake": 58.0, "color": Color(0.16, 0.62, 0.34)}
]

func _ready() -> void:
	add_to_group("vehicles")
	_apply_paint()

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

func cycle_tuning() -> String:
	tuning_index = (tuning_index + 1) % TUNING_PRESETS.size()
	var preset: Dictionary = TUNING_PRESETS[tuning_index]
	max_engine_force = float(preset["engine"])
	max_steering_angle = float(preset["steering"])
	max_brake_force = float(preset["brake"])
	paint_color = preset["color"]
	_apply_paint()
	return vehicle_label + " / " + str(preset["name"])

func _apply_paint() -> void:
	var body_mesh := get_node_or_null("Body") as MeshInstance3D
	if body_mesh == null:
		return
	var material := StandardMaterial3D.new()
	material.albedo_color = paint_color
	body_mesh.material_override = material
