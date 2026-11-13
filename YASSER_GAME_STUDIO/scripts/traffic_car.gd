extends Node3D

var route_points: Array[Vector3] = []
var route_index: int = 1
var cruise_speed: float = 7.0
var paint_color: Color = Color(0.18, 0.42, 0.72)

func configure(points: Array, speed: float, color: Color) -> void:
	route_points.clear()
	for point in points:
		if point is Vector3:
			route_points.append(point)
	cruise_speed = speed
	paint_color = color
	if not route_points.is_empty():
		global_position = route_points[0]

func _ready() -> void:
	add_to_group("traffic")
	_build_visual()

func _process(delta: float) -> void:
	if route_points.size() < 2:
		return
	var destination := route_points[route_index]
	var offset := destination - global_position
	offset.y = 0.0
	if offset.length() < 2.0:
		route_index = (route_index + 1) % route_points.size()
		destination = route_points[route_index]
		offset = destination - global_position
		offset.y = 0.0
	if offset.length_squared() < 0.01:
		return
	var direction := offset.normalized()
	global_position += direction * cruise_speed * delta
	look_at(global_position + direction, Vector3.UP)

func _build_visual() -> void:
	var body := MeshInstance3D.new()
	var body_mesh := BoxMesh.new()
	body_mesh.size = Vector3(1.7, 0.8, 3.2)
	body.mesh = body_mesh
	body.position.y = 0.62
	var body_material := StandardMaterial3D.new()
	body_material.albedo_color = paint_color
	body.material_override = body_material
	add_child(body)

	var roof := MeshInstance3D.new()
	var roof_mesh := BoxMesh.new()
	roof_mesh.size = Vector3(1.35, 0.48, 1.55)
	roof.mesh = roof_mesh
	roof.position = Vector3(0, 1.22, -0.12)
	var glass := StandardMaterial3D.new()
	glass.albedo_color = Color(0.12, 0.19, 0.24)
	roof.material_override = glass
	add_child(roof)
