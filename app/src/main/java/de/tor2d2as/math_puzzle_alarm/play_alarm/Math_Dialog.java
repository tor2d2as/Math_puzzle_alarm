package de.tor2d2as.math_puzzle_alarm.play_alarm;

import android.app.Dialog;
import android.content.Context;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.DialogFragment;

import java.util.Random;

import de.tor2d2as.math_puzzle_alarm.R;

public class Math_Dialog extends DialogFragment implements View.OnClickListener {

    private TextView math_exercise;
    private EditText user_solution_math;
    private Button dialog_button;
    private int correct_result;
    private boolean snooze_mode = false;
    private NotifyUi notifyUi;
    private Dialog dialog;

    /**
     * Opens the Dialog for the challenge.
     * @param context The context of the application.
     */
    private void show_Dialog(Context context){
        View rootView = LayoutInflater.from(context).inflate(R.layout.math_exercise_dialog, null);

        dialog = new Dialog(context);
        dialog.setContentView(rootView);
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        // Set up the views and handle button click
        math_exercise = rootView.findViewById(R.id.math_exercise);
        user_solution_math = rootView.findViewById(R.id.user_solution_math);
        user_solution_math.requestFocus();
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        dialog_button = rootView.findViewById(R.id.dialog_button);
        dialog_button.setOnClickListener(this);

        generate_exercise();
        dialog.show();

        user_solution_math.setOnKeyListener((v, keyCode, event) -> {
            if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                dialog_button_action(context);
                return true;
            }else {
                return false;
            }
        });
    }

    /**
     * Call this method for the 'Snooze Dialog'. After the user succeeds the
     * challenge, the interface: NotifyUi.snooze_alarm() will be called.
     * @param context The context of the application.
     * @param mNotifyUi The interface which should be called after the user succeeds the challenge.
     */
    public void snooze_alarm(Context context, NotifyUi mNotifyUi){
        show_Dialog(context);
        notifyUi = mNotifyUi;
        dialog_button.setText(context.getString(R.string.snooze));
        snooze_mode = true;
    }

    /**
     * Call this method for the 'Stop Dialog'. After the user succeeds the
     * challenge, the interface: NotifyUi.stop_alarm() will be called.
     * @param context The context of the application.
     * @param mNotifyUi The interface which should be called after the user succeeds the challenge.
     */
    public void stop_alarm(Context context, NotifyUi mNotifyUi){
        show_Dialog(context);
        notifyUi = mNotifyUi;
        dialog_button.setText(context.getString(R.string.stop_alarm));
        snooze_mode = false;
    }

    @Override
    public void onClick(View view) {
        if(view.getId() == dialog_button.getId()){
            dialog_button_action(view.getContext());
        }
    }

    /**
     * It handles the action if the user presses Enter or the Snooze/Stop button inside the dialog.
     * @param context An Android context that allows to show Toasts.
     */
    private void dialog_button_action(Context context){
        if(checkUserAnswer()){
            //User got the correct answer
            if(snooze_mode){
                notifyUi.snooze_alarm();
            }else{
                notifyUi.stop_alarm();
            }
            dialog.cancel();
        }else{
            //User got a wrong answer
            if(snooze_mode){
                Toast.makeText(context, context.getString(R.string.solve_exercise_snooze), Toast.LENGTH_SHORT).show();
            }else{
                Toast.makeText(context, context.getString(R.string.solve_exercise_first_stop), Toast.LENGTH_SHORT).show();
            }
            generate_exercise();
        }
    }

    /**
     * Checks if the user has solved the maths exercise correctly.
     * @return true = The user has given the correct answer!
     * false = The user has given a wrong answer.
     */
    private boolean checkUserAnswer(){
        int userResult;
        try {
            userResult = Integer.parseInt(user_solution_math.getText().toString());
        }catch (Exception e){
            //The result is no number, and with that, always wrong.
            return false;
        }
        return userResult == correct_result;
    }

    /**
     * Generates a maths exercise, writes the exercise in the EditText, and saves the correct
     * result in the variable correct_result.
     */
    private void generate_exercise() {
        Random rand = new Random();
        int math_sign = rand.nextInt(3);

        int number1;
        int number2;
        if (math_sign == 2) {
            //It is a multiplication exercise
            number1 = rand.nextInt(10) + 2;
            number2 = rand.nextInt(10) + 2;
        }else {
            number1 = rand.nextInt(1000) + 2;
            number2 = rand.nextInt(1000) + 2;
        }

        switch (math_sign) {
            case 0:
                math_exercise.setText(String.format("%s + %s =", number1, number2));
                correct_result = number1 + number2;
                break;
            case 1:
                if (number2 > number1){
                    //Switch number 1 and 2, so the result will be positive
                    //(the user is unable to type a negative result)
                    int tmp = number1;
                    number1 = number2;
                    number2 = tmp;
                }
                math_exercise.setText(String.format("%s - %s =", number1, number2));
                correct_result = number1 - number2;
                break;
            default:
                //This case only happens if the random generator chooses a 2
                math_exercise.setText(String.format("%s * %s =", number1, number2));
                correct_result = number1 * number2;
                break;
        }

    }

    public interface NotifyUi {
        /**
         * It will be called if the alarm should snooze.
         */
        void snooze_alarm();
        /**
         * It will be called if the alarm should stop.
         */
        void stop_alarm();
    }
}
