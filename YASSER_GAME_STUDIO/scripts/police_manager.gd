extends Node3D

signal wanted_changed(level: int)

const POLICE_SCRIPT = preload("res://scripts/police_unit.gd")
const MAX_WANTED_LEVEL := 3

var target: Node3D
var wanted_level: int = 0
var calm_timer: float = 0.0
var last_crime_msec: int = -10000
var police_units: Array[Node] = []

func configure(player: Node3D) -> void:
	target = player

func _process(delta: float) -> void:
	if wanted_level <= 0:
		return
	calm_timer = maxf(0.0, calm_timer - delta)
	if calm_timer <= 0.0:
		wanted_level = maxi(0, wanted_level - 1)
		calm_timer = 32.0
		_sync_police()
		wanted_changed.emit(wanted_level)

func report_crime(_crime_position: Vector3) -> void:
	var now := Time.get_ticks_msec()
	if now - last_crime_msec < 2200:
		return
	last_crime_msec = now
	wanted_level = mini(MAX_WANTED_LEVEL, wanted_level + 1)
	calm_timer = 42.0
	_sync_police()
	wanted_changed.emit(wanted_level)

func _sync_police() -> void:
	var active_units: Array[Node] = []
	for unit in police_units:
		if is_instance_valid(unit):
			active_units.append(unit)
	police_units = active_units

	while police_units.size() > wanted_level:
		var unit: Node = police_units.pop_back()
		if is_instance_valid(unit):
			unit.queue_free()

	while police_units.size() < wanted_level:
		if not is_instance_valid(target):
			return
		var index := police_units.size()
		var direction := Vector3.RIGHT.rotated(Vector3.UP, float(index) * TAU / float(MAX_WANTED_LEVEL))
		var spawn_position := target.global_position + direction * 19.0
		var officer: Variant = POLICE_SCRIPT.new()
		officer.call("configure", target, 7.0 + float(wanted_level) * 1.2)
		officer.global_position = spawn_position
		add_child(officer)
		police_units.append(officer)
