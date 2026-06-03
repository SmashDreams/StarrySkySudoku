from .models import AppState, DetectionResult


def next_action_for_detection(detection: DetectionResult) -> str:
    if detection.state == AppState.OUT_OF_APP:
        return "recover_to_app"
    if detection.state == AppState.UNKNOWN:
        return "retry_or_back"
    if detection.state == AppState.SPLASH:
        return "wait_or_continue"
    if detection.state == AppState.GUIDE:
        return "run_guide_step"
    if detection.state == AppState.MAP:
        return "enter_next_level"
    if detection.state == AppState.PLAY:
        return "play_current_level"
    if detection.state == AppState.PLAY_DIALOG:
        return "handle_dialog"
    return "bootstrap"
