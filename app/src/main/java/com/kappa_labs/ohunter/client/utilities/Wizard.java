package com.kappa_labs.ohunter.client.utilities;

import android.app.Activity;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;

import com.kappa_labs.ohunter.client.R;
import com.kappa_labs.ohunter.client.activities.HuntActivity;

/**
 * Class providing instructions for the player throughout the game.
 */
public class Wizard {

    private static final int NOTIFICATION_PHOTOGENIFY_ID = 0x100;


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

    private static void commitFragment(Context context, DialogFragment dialogFragment) {
        FragmentTransaction transaction = null;
        Activity activity = null;
        if (context instanceof AppCompatActivity) {
            activity = ((AppCompatActivity) context);
            transaction = ((AppCompatActivity) context).getSupportFragmentManager().beginTransaction();
        } else if (context instanceof FragmentActivity) {
            activity = ((FragmentActivity) context);
            transaction = ((FragmentActivity) context).getSupportFragmentManager().beginTransaction();
        }
        if (transaction != null && !activity.isFinishing() && !activity.isDestroyed()) {
            transaction.add(dialogFragment, "tag");
            transaction.commitAllowingStateLoss();
        }
    }

    public static DialogFragment gameInitializedDialog(Context context) {
        if (context == null) {
            return null;
        }
        BasicDialogFragment dialogFragment = new BasicDialogFragment();
        dialogFragment.setText(
                context.getString(R.string.dialog_wizard_game_initiated_title),
                context.getString(R.string.dialog_wizard_game_initiated_message));
        dialogFragment.setPositive(context.getString(R.string.dialog_wizard_game_initiated_positive), null);
        commitFragment(context, dialogFragment);
        return dialogFragment;
    }

    public static DialogFragment noTargetAvailableDialog(Context context, DialogInterface.OnClickListener positiveListener) {
        BasicDialogFragment dialogFragment = new BasicDialogFragment();
        dialogFragment.setText(
                context.getString(R.string.dialog_wizard_no_target_title),
                context.getString(R.string.dialog_wizard_no_target_message));
        dialogFragment.setPositive(context.getString(R.string.dialog_wizard_no_target_positive), positiveListener);
        dialogFragment.setCancelable(false);
        commitFragment(context, dialogFragment);
        return dialogFragment;
    }

    public static DialogFragment notEnoughAcceptableDialog(Context context) {
        BasicDialogFragment dialogFragment = new BasicDialogFragment();
        dialogFragment.setText(
                context.getString(R.string.dialog_wizard_low_acceptable_title),
                context.getString(R.string.dialog_wizard_low_acceptable_message));
        dialogFragment.setPositive(context.getString(R.string.dialog_wizard_low_acceptable_positive), null);
        commitFragment(context, dialogFragment);
        return dialogFragment;
    }

    public static DialogFragment acceptQuestionDialog(Context context, int amount, DialogInterface.OnClickListener positiveListener) {
        BasicDialogFragment dialogFragment = new BasicDialogFragment();
        dialogFragment.setText(
                context.getString(R.string.dialog_wizard_accept_question_title),
                String.format(context.getString(R.string.dialog_wizard_accept_question_message), amount));
        dialogFragment.setPositive(context.getString(R.string.dialog_wizard_accept_question_positive), positiveListener);
        dialogFragment.setNegative(context.getString(R.string.dialog_wizard_accept_question_negative), null);
        commitFragment(context, dialogFragment);
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
        commitFragment(context, dialogFragment);
        return dialogFragment;
    }

    public static DialogFragment deferQuestionDialog(Context context, int points, DialogInterface.OnClickListener positiveListener) {
        BasicDialogFragment dialogFragment = new BasicDialogFragment();
        dialogFragment.setText(
                context.getString(R.string.dialog_wizard_defer_question_title),
                String.format(context.getString(R.string.dialog_wizard_defer_question_message),
                        context.getResources().getQuantityString(R.plurals.numberOfPoints, points, points)));
        dialogFragment.setPositive(context.getString(R.string.dialog_wizard_defer_question_positive), positiveListener);
        dialogFragment.setNegative(context.getString(R.string.dialog_wizard_defer_question_negative), null);
        commitFragment(context, dialogFragment);
        return dialogFragment;
    }

    public static DialogFragment missingPointsDialog(Context context, int points) {
        BasicDialogFragment dialogFragment = new BasicDialogFragment();
        dialogFragment.setText(
                context.getString(R.string.dialog_wizard_missing_points_title),
                String.format(context.getString(R.string.dialog_wizard_missing_points_message),
                        context.getResources().getQuantityString(R.plurals.numberOfPoints, points, points)));
        dialogFragment.setPositive(context.getString(R.string.dialog_wizard_missing_points_positive), null);
        commitFragment(context, dialogFragment);
        return dialogFragment;
    }

    public static DialogFragment storeForEvaluationDialog(Context context, DialogInterface.OnClickListener positiveListener, DialogInterface.OnClickListener neutralListener, DialogInterface.OnClickListener negativeListener) {
        BasicDialogFragment dialogFragment = new BasicDialogFragment();
        dialogFragment.setText(
                context.getString(R.string.dialog_wizard_store_compare_title),
                context.getString(R.string.dialog_wizard_store_compare_message));
        dialogFragment.setPositive(context.getString(R.string.dialog_wizard_store_compare_positive), positiveListener);
        dialogFragment.setNeutral(context.getString(R.string.dialog_wizard_store_compare_neutral), neutralListener);
        dialogFragment.setNegative(context.getString(R.string.dialog_wizard_store_compare_negative), negativeListener);
        commitFragment(context, dialogFragment);
        return dialogFragment;
    }

    public static DialogFragment targetCompletedDialog(Context context) {
        BasicDialogFragment dialogFragment = new BasicDialogFragment();
        dialogFragment.setText(
                context.getString(R.string.dialog_wizard_target_completed_title),
                context.getString(R.string.dialog_wizard_target_completed_message));
        dialogFragment.setPositive(context.getString(R.string.dialog_wizard_target_completed_positive), null);
        commitFragment(context, dialogFragment);
        return dialogFragment;
    }

    public static void showPhotogenifiedNotification(Context context) {
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(context)
                        .setSmallIcon(R.drawable.ic_camera)
                        .setContentTitle(context.getString(R.string.notification_photogenified_title))
                        .setContentText(context.getString(R.string.notification_photogenified_text));
        Intent resultIntent = new Intent(context, HuntActivity.class);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addParentStack(HuntActivity.class);
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(resultPendingIntent);
        NotificationManagerCompat mNotificationManager = NotificationManagerCompat.from(context);
        mNotificationManager.notify(NOTIFICATION_PHOTOGENIFY_ID, mBuilder.build());
    }

}
