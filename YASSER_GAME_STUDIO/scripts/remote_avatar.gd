extends Node3D

var target_position: Vector3 = Vector3.ZERO
var target_yaw: float = 0.0

func _ready() -> void:
	var mesh_instance := MeshInstance3D.new()
	var capsule := CapsuleMesh.new()
	capsule.radius = 0.36
	capsule.height = 1.8
	mesh_instance.mesh = capsule
	mesh_instance.position.y = 0.9
	var material := StandardMaterial3D.new()
	material.albedo_color = Color(0.12, 0.62, 0.95)
	mesh_instance.material_override = material
	add_child(mesh_instance)

func set_network_transform(new_position: Vector3, new_yaw: float) -> void:
	target_position = new_position
	target_yaw = new_yaw

func _process(delta: float) -> void:
	global_position = global_position.lerp(target_position, minf(1.0, delta * 12.0))
	rotation.y = lerp_angle(rotation.y, target_yaw, minf(1.0, delta * 12.0))
