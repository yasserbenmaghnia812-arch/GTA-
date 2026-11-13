extends StaticBody3D

@export var health: float = 100.0

func take_damage(amount: float) -> void:
	health -= amount
	if health <= 0.0:
		queue_free()
