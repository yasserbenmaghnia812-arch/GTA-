extends Node3D

const PLAYER_SCENE = preload("res://scenes/player.tscn")
const VEHICLE_SCENE = preload("res://scenes/vehicle.tscn")
const HUD_SCRIPT = preload("res://scripts/hud.gd")
const REMOTE_SCRIPT = preload("res://scripts/remote_avatar.gd")
const TARGET_SCRIPT = preload("res://scripts/target.gd")
const MISSION_SCRIPT = preload("res://scripts/mission_manager.gd")

var player: CharacterBody3D
var vehicle: VehicleBody3D
var hud: CanvasLayer
var mission_manager: Node3D
var remote_players: Dictionary = {}
var _network_timer: float = 0.0
var _rng := RandomNumberGenerator.new()

func _ready() -> void:
	_setup_input()
	_rng.randomize()
	_build_world()

	player = PLAYER_SCENE.instantiate()
	add_child(player)
	player.global_position = Vector3(0, 1.0, 9)
	player.add_to_group("player")

	vehicle = VEHICLE_SCENE.instantiate()
	add_child(vehicle)
	vehicle.global_position = Vector3(3.0, 0.55, 9.0)

	hud = HUD_SCRIPT.new()
	add_child(hud)
	hud.host_requested.connect(host_game)
	hud.join_requested.connect(join_game)
	mission_manager = MISSION_SCRIPT.new()
	add_child(mission_manager)
	mission_manager.objective_updated.connect(hud.set_objective)
	mission_manager.mission_completed.connect(_on_mission_completed)
	_set_status("SOLO MODE  |  E: ENTER VEHICLE  |  F: FIRE")

func _process(delta: float) -> void:
	_network_timer += delta
	if _network_timer >= 0.06:
		_network_timer = 0.0
		if player != null and multiplayer.get_peers().size() > 0:
			if multiplayer.is_server():
				rpc("relay_remote", multiplayer.get_unique_id(), player.global_position, player.rotation.y)
			else:
				rpc_id(1, "server_receive_transform", multiplayer.get_unique_id(), player.global_position, player.rotation.y)

func _setup_input() -> void:
	_bind_key("move_forward", KEY_W)
	_bind_key("move_back", KEY_S)
	_bind_key("move_left", KEY_A)
	_bind_key("move_right", KEY_D)
	_bind_key("sprint", KEY_SHIFT)
	_bind_key("jump", KEY_SPACE)
	_bind_key("interact", KEY_E)
	_bind_key("fire", KEY_F)
	_bind_key("vehicle_forward", KEY_W)
	_bind_key("vehicle_reverse", KEY_S)
	_bind_key("vehicle_left", KEY_A)
	_bind_key("vehicle_right", KEY_D)
	_bind_key("vehicle_brake", KEY_SPACE)

func _bind_key(action_name: String, keycode: int) -> void:
	if not InputMap.has_action(action_name):
		InputMap.add_action(action_name)
	for existing in InputMap.action_get_events(action_name):
		if existing is InputEventKey and existing.keycode == keycode:
			return
	var event := InputEventKey.new()
	event.keycode = keycode
	InputMap.action_add_event(action_name, event)

func _build_world() -> void:
	var environment := WorldEnvironment.new()
	var env := Environment.new()
	env.background_mode = Environment.BG_COLOR
	env.background_color = Color(0.43, 0.63, 0.78)
	env.ambient_light_source = Environment.AMBIENT_SOURCE_COLOR
	env.ambient_light_color = Color(0.78, 0.82, 0.86)
	env.ambient_light_energy = 0.72
	environment.environment = env
	add_child(environment)

	var sunlight := DirectionalLight3D.new()
	sunlight.rotation_degrees = Vector3(-48, -28, 0)
	sunlight.light_energy = 1.15
	sunlight.shadow_enabled = false
	add_child(sunlight)

	var ground := StaticBody3D.new()
	ground.position.y = -0.5
	var floor_mesh := MeshInstance3D.new()
	var floor := BoxMesh.new()
	floor.size = Vector3(190, 1, 190)
	floor_mesh.mesh = floor
	floor_mesh.position.y = 0.5
	var floor_material := StandardMaterial3D.new()
	floor_material.albedo_color = Color(0.20, 0.28, 0.24)
	floor_mesh.material_override = floor_material
	ground.add_child(floor_mesh)
	var floor_collision := CollisionShape3D.new()
	var floor_shape := BoxShape3D.new()
	floor_shape.size = Vector3(190, 1, 190)
	floor_collision.shape = floor_shape
	ground.add_child(floor_collision)
	add_child(ground)

	_add_road(Vector3(0, 0.025, 0), Vector3(12, 0.05, 180))
	_add_road(Vector3(0, 0.03, 0), Vector3(180, 0.05, 12))
	for lane_offset in [-2.2, 2.2]:
		_add_road(Vector3(lane_offset, 0.06, 0), Vector3(0.12, 0.025, 180), Color(0.88, 0.78, 0.40))
		_add_road(Vector3(0, 0.06, lane_offset), Vector3(180, 0.025, 0.12), Color(0.88, 0.78, 0.40))

	for x in range(-2, 3):
		for z in range(-2, 3):
			if x == 0 or z == 0:
				continue
			var offset_x := float(x) * 28.0 + _rng.randf_range(-4.0, 4.0)
			var offset_z := float(z) * 28.0 + _rng.randf_range(-4.0, 4.0)
			var building_size := Vector3(_rng.randf_range(7.0, 11.0), _rng.randf_range(8.0, 22.0), _rng.randf_range(7.0, 11.0))
			_add_building(Vector3(offset_x, building_size.y * 0.5, offset_z), building_size)

	_add_target(Vector3(-5, 0, -8))
	_add_target(Vector3(5, 0, -14))
	_add_target(Vector3(0, 0, -22))

func _add_road(position: Vector3, size: Vector3, color: Color = Color(0.12, 0.13, 0.15)) -> void:
	var road := MeshInstance3D.new()
	var mesh := BoxMesh.new()
	mesh.size = size
	road.mesh = mesh
	road.position = position
	var material := StandardMaterial3D.new()
	material.albedo_color = color
	road.material_override = material
	add_child(road)

func _add_building(position: Vector3, size: Vector3) -> void:
	var body := StaticBody3D.new()
	body.position = position
	var mesh_instance := MeshInstance3D.new()
	var mesh := BoxMesh.new()
	mesh.size = size
	mesh_instance.mesh = mesh
	var material := StandardMaterial3D.new()
	var shade := _rng.randf_range(0.0, 0.12)
	material.albedo_color = Color(0.28 + shade, 0.34 + shade, 0.40 + shade)
	mesh_instance.material_override = material
	body.add_child(mesh_instance)
	var collision := CollisionShape3D.new()
	var shape := BoxShape3D.new()
	shape.size = size
	collision.shape = shape
	body.add_child(collision)
	add_child(body)

func _add_target(position: Vector3) -> void:
	var target := StaticBody3D.new()
	target.set_script(TARGET_SCRIPT)
	target.position = position
	var mesh_instance := MeshInstance3D.new()
	var mesh := BoxMesh.new()
	mesh.size = Vector3(0.7, 1.8, 0.7)
	mesh_instance.mesh = mesh
	var material := StandardMaterial3D.new()
	material.albedo_color = Color(0.9, 0.23, 0.12)
	mesh_instance.material_override = material
	mesh_instance.position.y = 0.9
	target.add_child(mesh_instance)
	var collision := CollisionShape3D.new()
	var shape := BoxShape3D.new()
	shape.size = Vector3(0.7, 1.8, 0.7)
	collision.shape = shape
	collision.position.y = 0.9
	target.add_child(collision)
	add_child(target)

func host_game() -> void:
	_disconnect_network()
	var peer := ENetMultiplayerPeer.new()
	var error := peer.create_server(7777, 8)
	if error != OK:
		_set_status("HOST ERROR: " + str(error))
		return
	multiplayer.multiplayer_peer = peer
	if not multiplayer.peer_connected.is_connected(_on_peer_connected):
		multiplayer.peer_connected.connect(_on_peer_connected)
	if not multiplayer.peer_disconnected.is_connected(_on_peer_disconnected):
		multiplayer.peer_disconnected.connect(_on_peer_disconnected)
	_set_status("HOSTING UDP 7777  |  MAX 8 PLAYERS")

func join_game(address: String) -> void:
	if address.is_empty():
		_set_status("ENTER A HOST IP ADDRESS")
		return
	_disconnect_network()
	var peer := ENetMultiplayerPeer.new()
	var error := peer.create_client(address, 7777)
	if error != OK:
		_set_status("JOIN ERROR: " + str(error))
		return
	multiplayer.multiplayer_peer = peer
	if not multiplayer.connected_to_server.is_connected(_on_connected_to_server):
		multiplayer.connected_to_server.connect(_on_connected_to_server)
	if not multiplayer.connection_failed.is_connected(_on_connection_failed):
		multiplayer.connection_failed.connect(_on_connection_failed)
	if not multiplayer.server_disconnected.is_connected(_on_server_disconnected):
		multiplayer.server_disconnected.connect(_on_server_disconnected)
	_set_status("CONNECTING TO " + address + ":7777")

func _disconnect_network() -> void:
	if multiplayer.multiplayer_peer != null:
		multiplayer.multiplayer_peer.close()
	multiplayer.multiplayer_peer = OfflineMultiplayerPeer.new()
	for peer_id in remote_players.keys():
		var avatar: Node = remote_players[peer_id]
		if is_instance_valid(avatar):
			avatar.queue_free()
	remote_players.clear()

func _on_peer_connected(peer_id: int) -> void:
	rpc("spawn_remote", peer_id)
	_set_status("PLAYER CONNECTED  |  ID " + str(peer_id))

func _on_peer_disconnected(peer_id: int) -> void:
	rpc("remove_remote", peer_id)
	_set_status("PLAYER LEFT  |  ID " + str(peer_id))

func _on_connected_to_server() -> void:
	rpc_id(1, "request_world_state")
	_set_status("CONNECTED TO HOST  |  UDP 7777")

func _on_connection_failed() -> void:
	_set_status("CONNECTION FAILED  |  CHECK IP / PORT")

func _on_server_disconnected() -> void:
	_set_status("SERVER DISCONNECTED")

@rpc("any_peer", "reliable")
func request_world_state() -> void:
	if not multiplayer.is_server():
		return
	var requester := multiplayer.get_remote_sender_id()
	rpc_id(requester, "spawn_remote", multiplayer.get_unique_id())
	for peer_id in multiplayer.get_peers():
		if peer_id != requester:
			rpc_id(requester, "spawn_remote", peer_id)

@rpc("authority", "call_local", "reliable")
func spawn_remote(peer_id: int) -> void:
	if peer_id == multiplayer.get_unique_id() or remote_players.has(peer_id):
		return
	var avatar: Node3D = REMOTE_SCRIPT.new()
	avatar.name = "RemotePlayer_" + str(peer_id)
	add_child(avatar)
	remote_players[peer_id] = avatar

@rpc("authority", "call_local", "reliable")
func remove_remote(peer_id: int) -> void:
	if remote_players.has(peer_id):
		var avatar: Node = remote_players[peer_id]
		if is_instance_valid(avatar):
			avatar.queue_free()
		remote_players.erase(peer_id)

@rpc("any_peer", "unreliable_ordered")
func server_receive_transform(peer_id: int, position: Vector3, yaw: float) -> void:
	if not multiplayer.is_server() or multiplayer.get_remote_sender_id() != peer_id:
		return
	_apply_remote_transform(peer_id, position, yaw)
	for connected_peer in multiplayer.get_peers():
		if connected_peer != peer_id:
			rpc_id(connected_peer, "relay_remote", peer_id, position, yaw)

@rpc("authority", "unreliable_ordered")
func relay_remote(peer_id: int, position: Vector3, yaw: float) -> void:
	_apply_remote_transform(peer_id, position, yaw)

func _apply_remote_transform(peer_id: int, position: Vector3, yaw: float) -> void:
	if not remote_players.has(peer_id):
		return
	var avatar: Node = remote_players[peer_id]
	if is_instance_valid(avatar):
		avatar.set_network_transform(position, yaw)

func _set_status(message: String) -> void:
	if is_instance_valid(hud):
		hud.set_status(message)

func _on_mission_completed(reward: int) -> void:
	_set_status("MISSION COMPLETE  |  +" + str(reward) + " CREDITS")
