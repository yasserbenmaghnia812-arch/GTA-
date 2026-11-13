extends Node3D

signal objective_updated(text: String)
signal mission_completed(reward: int)

const SAVE_PATH := "user://yasser_open_world_save.cfg"
const CHECKPOINTS: Array[Vector3] = [
	Vector3(0, 0.0, -34),
	Vector3(34, 0.0, 0),
	Vector3(-34, 0.0, 0)
]
const REWARD_PER_MISSION := 250

var mission_index: int = 0
var credits: int = 0
var checkpoint_area: Area3D
var checkpoint_visual: MeshInstance3D

func _ready() -> void:
	_load_progress()
	_create_checkpoint()
	_publish_objective()

func _process(delta: float) -> void:
	if is_instance_valid(checkpoint_visual):
		checkpoint_visual.rotate_y(delta * 0.8)

func _create_checkpoint() -> void:
	if mission_index >= CHECKPOINTS.size():
		return

	checkpoint_area = Area3D.new()
	checkpoint_area.name = "MissionCheckpoint"
	checkpoint_area.position = CHECKPOINTS[mission_index] + Vector3(0, 1.0, 0)
	checkpoint_area.collision_layer = 0
	checkpoint_area.collision_mask = 1
	checkpoint_area.body_entered.connect(_on_checkpoint_body_entered)

	var collision := CollisionShape3D.new()
	var sphere := SphereShape3D.new()
	sphere.radius = 3.0
	collision.shape = sphere
	checkpoint_area.add_child(collision)

	checkpoint_visual = MeshInstance3D.new()
	var ring := CylinderMesh.new()
	ring.top_radius = 2.0
	ring.bottom_radius = 2.0
	ring.height = 0.12
	checkpoint_visual.mesh = ring
	checkpoint_visual.position.y = -0.82
	var material := StandardMaterial3D.new()
	material.albedo_color = Color(0.12, 0.85, 0.95, 0.72)
	material.transparency = BaseMaterial3D.TRANSPARENCY_ALPHA
	material.emission_enabled = true
	material.emission = Color(0.05, 0.45, 0.65)
	checkpoint_visual.material_override = material
	checkpoint_area.add_child(checkpoint_visual)
	add_child(checkpoint_area)

func _on_checkpoint_body_entered(body: Node3D) -> void:
	if not body.is_in_group("player"):
		return

	credits += REWARD_PER_MISSION
	mission_index += 1
	_save_progress()
	mission_completed.emit(REWARD_PER_MISSION)

	if is_instance_valid(checkpoint_area):
		checkpoint_area.queue_free()
	checkpoint_area = null
	checkpoint_visual = null

	_create_checkpoint()
	_publish_objective()

func _publish_objective() -> void:
	if mission_index >= CHECKPOINTS.size():
		objective_updated.emit("ALL STARTER MISSIONS COMPLETE  |  CREDITS: $" + str(credits))
	else:
		objective_updated.emit("MISSION " + str(mission_index + 1) + "/" + str(CHECKPOINTS.size()) + "  |  REACH THE CYAN MARKER  |  $" + str(credits))

func _save_progress() -> void:
	var config := ConfigFile.new()
	config.set_value("progress", "mission_index", mission_index)
	config.set_value("progress", "credits", credits)
	config.save(SAVE_PATH)

func _load_progress() -> void:
	var config := ConfigFile.new()
	if config.load(SAVE_PATH) != OK:
		return
	mission_index = clampi(int(config.get_value("progress", "mission_index", 0)), 0, CHECKPOINTS.size())
	credits = maxi(0, int(config.get_value("progress", "credits", 0)))
