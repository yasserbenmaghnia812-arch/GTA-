extends CanvasLayer

signal host_requested
signal join_requested(address: String)

var status_label: Label
var address_input: LineEdit
var objective_label: Label

func _ready() -> void:
	var root := Control.new()
	root.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
	root.mouse_filter = Control.MOUSE_FILTER_PASS
	add_child(root)

	var screen := get_viewport().get_visible_rect().size
	var width := screen.x
	var height := screen.y

	objective_label = Label.new()
	objective_label.position = Vector2(width * 0.22, 58)
	objective_label.size = Vector2(width * 0.56, 34)
	objective_label.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
	objective_label.add_theme_color_override("font_color", Color(0.75, 0.98, 1.0))
	root.add_child(objective_label)

	status_label = Label.new()
	status_label.position = Vector2(width - 390, 14)
	status_label.size = Vector2(370, 38)
	status_label.text = "YASSER OPEN WORLD  |  SOLO"
	status_label.horizontal_alignment = HORIZONTAL_ALIGNMENT_RIGHT
	status_label.add_theme_color_override("font_color", Color(1, 1, 1))
	root.add_child(status_label)

	address_input = LineEdit.new()
	address_input.position = Vector2(18, 16)
	address_input.size = Vector2(150, 38)
	address_input.placeholder_text = "Host IP"
	address_input.text = "127.0.0.1"
	root.add_child(address_input)

	var host_button := _button("HOST 8", Vector2(176, 16), Vector2(92, 38))
	host_button.pressed.connect(func(): host_requested.emit())
	root.add_child(host_button)

	var join_button := _button("JOIN", Vector2(274, 16), Vector2(82, 38))
	join_button.pressed.connect(func(): join_requested.emit(address_input.text.strip_edges()))
	root.add_child(join_button)

	var center_x := 116.0
	var bottom := height - 176.0
	_add_action_button(root, "UP", Vector2(center_x, bottom - 66), ["move_forward", "vehicle_forward"])
	_add_action_button(root, "DOWN", Vector2(center_x, bottom + 66), ["move_back", "vehicle_reverse"])
	_add_action_button(root, "LEFT", Vector2(center_x - 70, bottom), ["move_left", "vehicle_left"])
	_add_action_button(root, "RIGHT", Vector2(center_x + 70, bottom), ["move_right", "vehicle_right"])

	_add_action_button(root, "RUN", Vector2(22, height - 90), ["sprint"], Vector2(84, 58))
	_add_action_button(root, "FIRE", Vector2(width - 118, height - 188), ["fire"], Vector2(100, 78))
	_add_action_button(root, "JUMP", Vector2(width - 224, height - 112), ["jump"], Vector2(88, 60))
	_add_action_button(root, "ENTER / EXIT", Vector2(width - 224, height - 42), ["interact"], Vector2(132, 54))
	_add_action_button(root, "BRAKE", Vector2(width - 118, height - 98), ["vehicle_brake"], Vector2(100, 54))

func _button(label: String, position: Vector2, size: Vector2) -> Button:
	var button := Button.new()
	button.text = label
	button.position = position
	button.size = size
	button.focus_mode = Control.FOCUS_NONE
	return button

func _add_action_button(root: Control, label: String, position: Vector2, actions: Array[String], size: Vector2 = Vector2(64, 58)) -> void:
	var button := _button(label, position, size)
	button.button_down.connect(_press_actions.bind(actions))
	button.button_up.connect(_release_actions.bind(actions))
	button.mouse_exited.connect(_release_actions.bind(actions))
	root.add_child(button)

func _press_actions(actions: Array[String]) -> void:
	for action in actions:
		Input.action_press(action)

func _release_actions(actions: Array[String]) -> void:
	for action in actions:
		Input.action_release(action)

func set_status(message: String) -> void:
	if is_instance_valid(status_label):
		status_label.text = message

func set_objective(message: String) -> void:
	if is_instance_valid(objective_label):
		objective_label.text = message
