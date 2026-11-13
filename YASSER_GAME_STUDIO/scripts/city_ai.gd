extends Node3D

const NPC_SCRIPT = preload("res://scripts/npc.gd")
const TRAFFIC_SCRIPT = preload("res://scripts/traffic_car.gd")

func _ready() -> void:
	_spawn_pedestrians()
	_spawn_traffic()

func _spawn_pedestrians() -> void:
	var routes: Array[Array] = [
		[Vector3(8.5, 0, -18), Vector3(8.5, 0, 18)],
		[Vector3(-8.5, 0, 20), Vector3(-8.5, 0, -20)],
		[Vector3(18, 0, 8.5), Vector3(-18, 0, 8.5)]
	]
	for route in routes:
		var pedestrian: CharacterBody3D = NPC_SCRIPT.new()
		pedestrian.set_patrol_points(route)
		add_child(pedestrian)

func _spawn_traffic() -> void:
	var north_south: Node3D = TRAFFIC_SCRIPT.new()
	north_south.configure(
		[Vector3(-3.2, 0, -72), Vector3(-3.2, 0, 72)],
		7.5,
		Color(0.18, 0.40, 0.72)
	)
	add_child(north_south)

	var east_west: Node3D = TRAFFIC_SCRIPT.new()
	east_west.configure(
		[Vector3(-72, 0, 3.2), Vector3(72, 0, 3.2)],
		6.5,
		Color(0.68, 0.28, 0.16)
	)
	add_child(east_west)
