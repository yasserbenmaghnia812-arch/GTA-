extends Node

signal status_changed(weapon_name: String, magazine: int, reserve: int)
signal crime_committed(position: Vector3)

const WEAPONS: Array[Dictionary] = [
	{"name": "PISTOL", "damage": 34.0, "interval": 0.32, "range": 75.0, "capacity": 12, "reload_time": 1.0, "pellets": 1, "spread": 0.004},
	{"name": "RIFLE", "damage": 22.0, "interval": 0.12, "range": 105.0, "capacity": 30, "reload_time": 1.45, "pellets": 1, "spread": 0.012},
	{"name": "SHOTGUN", "damage": 11.0, "interval": 0.78, "range": 38.0, "capacity": 6, "reload_time": 1.7, "pellets": 7, "spread": 0.075}
]

var magazine_ammo: Array[int] = [12, 30, 6]
var reserve_ammo: Array[int] = [72, 150, 36]
var active_weapon: int = 0
var fire_cooldown: float = 0.0
var reload_remaining: float = 0.0
var is_reloading: bool = false
var rng := RandomNumberGenerator.new()

func _ready() -> void:
	rng.randomize()
	publish_status()

func _process(delta: float) -> void:
	fire_cooldown = maxf(0.0, fire_cooldown - delta)
	if not is_reloading:
		return
	reload_remaining = maxf(0.0, reload_remaining - delta)
	if reload_remaining <= 0.0:
		var data: Dictionary = WEAPONS[active_weapon]
		var needed: int = int(data["capacity"]) - magazine_ammo[active_weapon]
		var transferred: int = mini(needed, reserve_ammo[active_weapon])
		magazine_ammo[active_weapon] += transferred
		reserve_ammo[active_weapon] -= transferred
		is_reloading = false
		publish_status()

func next_weapon() -> void:
	active_weapon = (active_weapon + 1) % WEAPONS.size()
	is_reloading = false
	reload_remaining = 0.0
	publish_status()

func reload() -> void:
	if is_reloading:
		return
	var data: Dictionary = WEAPONS[active_weapon]
	if magazine_ammo[active_weapon] >= int(data["capacity"]) or reserve_ammo[active_weapon] <= 0:
		return
	is_reloading = true
	reload_remaining = float(data["reload_time"])
	publish_status()

func try_fire(camera: Camera3D, shooter: Node3D) -> bool:
	if is_reloading or fire_cooldown > 0.0:
		return false
	if magazine_ammo[active_weapon] <= 0:
		reload()
		return false

	var data: Dictionary = WEAPONS[active_weapon]
	magazine_ammo[active_weapon] -= 1
	fire_cooldown = float(data["interval"])

	var forward := -camera.global_transform.basis.z
	var right := camera.global_transform.basis.x
	var up := camera.global_transform.basis.y
	var pellet_count := int(data["pellets"])
	for pellet_index in range(pellet_count):
		var spread := float(data["spread"])
		var direction := (forward
			+ right * rng.randf_range(-spread, spread)
			+ up * rng.randf_range(-spread, spread)).normalized()
		var origin := camera.global_position
		var destination := origin + direction * float(data["range"])
		var query := PhysicsRayQueryParameters3D.create(origin, destination)
		query.exclude = [shooter.get_rid()]
		var hit := shooter.get_world_3d().direct_space_state.intersect_ray(query)
		if hit.is_empty():
			continue

		var collider: Object = hit.get("collider")
		if collider == null or not collider is Node:
			continue
		var hit_node := collider as Node
		if hit_node.is_in_group("npcs"):
			crime_committed.emit(hit.get("position", origin))
		if hit_node.is_in_group("police"):
			crime_committed.emit(hit.get("position", origin))
		if hit_node.has_method("take_damage"):
			hit_node.take_damage(float(data["damage"]))

	publish_status()
	return true

func publish_status() -> void:
	var data: Dictionary = WEAPONS[active_weapon]
	status_changed.emit(
		str(data["name"]),
		magazine_ammo[active_weapon],
		reserve_ammo[active_weapon]
	)
