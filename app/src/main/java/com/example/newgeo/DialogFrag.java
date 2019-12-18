package com.example.newgeo;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDialogFragment;

public class DialogFrag extends AppCompatDialogFragment {
    private EditText reminder;
    private EditText radius;
    private DialogFragListener listener;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.layout_dialog,null);
        builder.setView(view)
                .setTitle("Create Reminder")
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                })
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String info = reminder.getText().toString();
                        String rad = radius.getText().toString();
                        listener.applyText(info,rad);
                    }
                });
        reminder = view.findViewById(R.id.info);
        radius = view.findViewById(R.id.radius);
        return builder.create();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            listener = (DialogFragListener) context;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public interface  DialogFragListener{
        void applyText(String info, String radius);
    }
}
