package com.kappa_labs.ohunter.client.utilities;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;

import com.kappa_labs.ohunter.client.R;

/**
 * Class providing instructions for the player throughout the game.
 */
public class Wizard {

    public static class BasicDialogFragment extends DialogFragment {

        private String mTitle, mMessage;
        private String mPositive, mNeutral, mNegative;
        private DialogInterface.OnClickListener mPositiveListener, mNeutralListener, mNegativeListener;

        public void setText(String title, String message) {
            this.mTitle = title;
            this.mMessage = message;
        }

        public void setPositive(String positive, DialogInterface.OnClickListener positiveListener) {
            this.mPositive = positive;
            this.mPositiveListener = positiveListener;
        }

        @SuppressWarnings("unused")
        public void setNeutral(String neutral, DialogInterface.OnClickListener neutralListener) {
            this.mNeutral = neutral;
            this.mNeutralListener = neutralListener;
        }

        public void setNegative(String negative, DialogInterface.OnClickListener negativeListener) {
            this.mNegative = negative;
            this.mNegativeListener = negativeListener;
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage(mMessage).setTitle(mTitle);
            if (mPositive != null) {
                builder.setPositiveButton(mPositive, mPositiveListener);
            }
            if (mNeutral != null) {
                builder.setNeutralButton(mNeutral, mNeutralListener);
            }
            if (mNegative != null) {
                builder.setNegativeButton(mNegative, mNegativeListener);
            }
            setRetainInstance(true);
            return builder.create();
        }

        @Override
        public void onDestroyView() {
            /* NOTE: Hack for bug https://code.google.com/p/android/issues/detail?id=17423 */
            if (getDialog() != null && getRetainInstance()) {
                getDialog().setDismissMessage(null);
            }
            super.onDestroyView();
        }

    }

    public static DialogFragment gameInitializedDialog(Context context) {
        BasicDialogFragment dialogFragment = new BasicDialogFragment();
        dialogFragment.setText(
                context.getString(R.string.dialog_wizard_game_initiated_title),
                context.getString(R.string.dialog_wizard_game_initiated_message));
        dialogFragment.setPositive(context.getString(R.string.dialog_wizard_game_initiated_positive), null);
        return dialogFragment;
    }

    public static DialogFragment noTargetAvailableDialog(Context context, DialogInterface.OnClickListener positiveListener) {
        BasicDialogFragment dialogFragment = new BasicDialogFragment();
        dialogFragment.setText(
                context.getString(R.string.dialog_wizard_no_target_title),
                context.getString(R.string.dialog_wizard_no_target_message));
        dialogFragment.setPositive(context.getString(R.string.dialog_wizard_no_target_positive), positiveListener);
        dialogFragment.setCancelable(false);
        return dialogFragment;
    }

    public static DialogFragment notEnoughAcceptableDialog(Context context) {
        BasicDialogFragment dialogFragment = new BasicDialogFragment();
        dialogFragment.setText(
                context.getString(R.string.dialog_wizard_low_acceptable_title),
                context.getString(R.string.dialog_wizard_low_acceptable_message));
        dialogFragment.setPositive(context.getString(R.string.dialog_wizard_low_acceptable_positive), null);
        return dialogFragment;
    }

    public static DialogFragment acceptQuestionDialog(Context context, int amount, DialogInterface.OnClickListener positiveListener) {
        BasicDialogFragment dialogFragment = new BasicDialogFragment();
        dialogFragment.setText(
                context.getString(R.string.dialog_wizard_accept_question_title),
                String.format(context.getString(R.string.dialog_wizard_accept_question_message), amount));
        dialogFragment.setPositive(context.getString(R.string.dialog_wizard_accept_question_positive), positiveListener);
        dialogFragment.setNegative(context.getString(R.string.dialog_wizard_accept_question_negative), null);
        return dialogFragment;
    }

    public static DialogFragment rejectQuestionDialog(Context context, int points, DialogInterface.OnClickListener positiveListener) {
        BasicDialogFragment dialogFragment = new BasicDialogFragment();
        dialogFragment.setText(
                context.getString(R.string.dialog_wizard_reject_question_title),
                String.format(context.getString(R.string.dialog_wizard_reject_question_message),
                        context.getResources().getQuantityString(R.plurals.numberOfPoints, points, points)));
        dialogFragment.setPositive(context.getString(R.string.dialog_wizard_reject_question_positive), positiveListener);
        dialogFragment.setNegative(context.getString(R.string.dialog_wizard_reject_question_negative), null);
        return dialogFragment;
    }

}
