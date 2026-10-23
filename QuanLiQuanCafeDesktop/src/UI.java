import javafx.scene.control.Dialog;

// Tiện ích dùng chung: áp giao diện tối (style.css) cho các hộp thoại Alert / TextInputDialog / ChoiceDialog
public class UI {
    public static void style(Dialog<?> dialog) {
        dialog.getDialogPane().getStylesheets().add(UI.class.getResource("style.css").toExternalForm());
    }
}
