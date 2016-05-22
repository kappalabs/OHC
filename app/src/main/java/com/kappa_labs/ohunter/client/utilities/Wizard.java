package com.kappa_labs.ohunter.client.utilities;

import android.app.Activity;
import android.app.Dialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
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
import android.util.Log;
import android.widget.Toast;

import com.kappa_labs.ohunter.client.R;
import com.kappa_labs.ohunter.client.activities.HuntActivity;
import com.kappa_labs.ohunter.lib.net.OHException;

/**
 * Class providing instructions for the player throughout the game.
 */
public class Wizard {

    private static final int NOTIFICATION_PHOTOGENIFY_ID = 0x100;

    private static DialogFragment targetCompletedDialog;


    public static class BasicInfoDialogFragment extends DialogFragment {

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

    public static class BasicProgressDialogFragment extends DialogFragment {

        private static String mTitle, mMessage;


        public void setTexts(String title, String message) {
            mTitle = title;
            mMessage = message;
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final ProgressDialog dialog = ProgressDialog.show(getActivity(), mTitle, mMessage, true);
            dialog.setCancelable(false);
            dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            setRetainInstance(true);

            return dialog;
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

    private static void commitFragment(Context context, DialogFragment dialogFragment, String tag) {
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
            transaction.add(dialogFragment, tag)
                    .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                    .commitAllowingStateLoss();
        }
    }

    public static DialogFragment gameInitializedDialog(Context context) {
        if (context == null || !SharedDataManager.showWizardOnNewHunt(context)) {
            return null;
        }
        BasicInfoDialogFragment dialogFragment = new BasicInfoDialogFragment();
        dialogFragment.setText(
                context.getString(R.string.dialog_wizard_game_initiated_title),
                context.getString(R.string.dialog_wizard_game_initiated_message));
        dialogFragment.setPositive(context.getString(R.string.dialog_wizard_game_initiated_positive), null);
        commitFragment(context, dialogFragment, "gameInitializedDialogTag");
        return dialogFragment;
    }

    public static DialogFragment noTargetAvailableDialog(Context context, DialogInterface.OnClickListener positiveListener) {
        if (context == null) {
            return null;
        }
        BasicInfoDialogFragment dialogFragment = new BasicInfoDialogFragment();
        dialogFragment.setText(
                context.getString(R.string.dialog_wizard_no_target_title),
                context.getString(R.string.dialog_wizard_no_target_message));
        dialogFragment.setPositive(context.getString(R.string.dialog_wizard_no_target_positive), positiveListener);
        dialogFragment.setCancelable(false);
        commitFragment(context, dialogFragment, "noTargetAvailableDialogTag");
        return dialogFragment;
    }

    public static DialogFragment notEnoughAcceptableDialog(Context context) {
        if (context == null) {
            return null;
        }
        BasicInfoDialogFragment dialogFragment = new BasicInfoDialogFragment();
        dialogFragment.setText(
                context.getString(R.string.dialog_wizard_low_acceptable_title),
                context.getString(R.string.dialog_wizard_low_acceptable_message));
        dialogFragment.setPositive(context.getString(R.string.dialog_wizard_low_acceptable_positive), null);
        commitFragment(context, dialogFragment, "notEnoughAcceptableDialogTag");
        return dialogFragment;
    }

    public static DialogFragment acceptQuestionDialog(Context context, int amount, DialogInterface.OnClickListener positiveListener) {
        if (context == null) {
            return null;
        }
        if (!SharedDataManager.showConfirmationOfAcceptTarget(context)) {
            positiveListener.onClick(null, 0);
            return null;
        }
        BasicInfoDialogFragment dialogFragment = new BasicInfoDialogFragment();
        dialogFragment.setText(
                context.getString(R.string.dialog_wizard_accept_question_title),
                String.format(context.getString(R.string.dialog_wizard_accept_question_message), amount));
        dialogFragment.setPositive(context.getString(R.string.dialog_wizard_accept_question_positive), positiveListener);
        dialogFragment.setNegative(context.getString(R.string.dialog_wizard_accept_question_negative), null);
        commitFragment(context, dialogFragment, "acceptQuestionDialogTag");
        return dialogFragment;
    }

    public static DialogFragment rejectQuestionDialog(Context context, int points, DialogInterface.OnClickListener positiveListener) {
        if (context == null) {
            return null;
        }
        if (!SharedDataManager.showConfirmationOfRejectTarget(context)) {
            positiveListener.onClick(null, 0);
            return null;
        }
        BasicInfoDialogFragment dialogFragment = new BasicInfoDialogFragment();
        dialogFragment.setText(
                context.getString(R.string.dialog_wizard_reject_question_title),
                String.format(context.getString(R.string.dialog_wizard_reject_question_message),
                        context.getResources().getQuantityString(R.plurals.numberOfPoints, points, points)));
        dialogFragment.setPositive(context.getString(R.string.dialog_wizard_reject_question_positive), positiveListener);
        dialogFragment.setNegative(context.getString(R.string.dialog_wizard_reject_question_negative), null);
        commitFragment(context, dialogFragment, "rejectQuestionDialogTag");
        return dialogFragment;
    }

    public static DialogFragment openUpQuestionDialog(Context context, int points, DialogInterface.OnClickListener positiveListener) {
        if (context == null) {
            return null;
        }
        if (!SharedDataManager.showConfirmationOfOpenUpTarget(context)) {
            positiveListener.onClick(null, 0);
            return null;
        }
        BasicInfoDialogFragment dialogFragment = new BasicInfoDialogFragment();
        dialogFragment.setText(
                context.getString(R.string.dialog_wizard_open_up_question_title),
                String.format(context.getString(R.string.dialog_wizard_open_up_question_message),
                        context.getResources().getQuantityString(R.plurals.numberOfPoints, points, points)));
        dialogFragment.setPositive(context.getString(R.string.dialog_wizard_open_up_question_positive), positiveListener);
        dialogFragment.setNegative(context.getString(R.string.dialog_wizard_open_up_question_negative), null);
        commitFragment(context, dialogFragment, "openUpQuestionDialogTag");
        return dialogFragment;
    }

    public static DialogFragment missingPointsDialog(Context context, int points) {
        if (context == null) {
            return null;
        }
        BasicInfoDialogFragment dialogFragment = new BasicInfoDialogFragment();
        dialogFragment.setText(
                context.getString(R.string.dialog_wizard_missing_points_title),
                String.format(context.getString(R.string.dialog_wizard_missing_points_message),
                        context.getResources().getQuantityString(R.plurals.numberOfPoints, points, points)));
        dialogFragment.setPositive(context.getString(R.string.dialog_wizard_missing_points_positive), null);
        commitFragment(context, dialogFragment, "missingPointsDialogTag");
        return dialogFragment;
    }

    public static DialogFragment storeForEvaluationDialog(Context context, DialogInterface.OnClickListener positiveListener, DialogInterface.OnClickListener neutralListener, DialogInterface.OnClickListener negativeListener) {
        if (context == null) {
            return null;
        }
        BasicInfoDialogFragment dialogFragment = new BasicInfoDialogFragment();
        dialogFragment.setText(
                context.getString(R.string.dialog_wizard_store_compare_title),
                context.getString(R.string.dialog_wizard_store_compare_message));
        dialogFragment.setPositive(context.getString(R.string.dialog_wizard_store_compare_positive), positiveListener);
        dialogFragment.setNeutral(context.getString(R.string.dialog_wizard_store_compare_neutral), neutralListener);
        dialogFragment.setNegative(context.getString(R.string.dialog_wizard_store_compare_negative), negativeListener);
        commitFragment(context, dialogFragment, "storeForEvaluationDialogTag");
        return dialogFragment;
    }

    public static DialogFragment targetLockedDialog(Context context) {
        if (context == null || !SharedDataManager.showWizardOnTargetLocked(context)) {
            return null;
        }
        BasicInfoDialogFragment dialogFragment = new BasicInfoDialogFragment();
        dialogFragment.setText(
                context.getString(R.string.dialog_wizard_target_locked_title),
                context.getString(R.string.dialog_wizard_target_locked_message));
        dialogFragment.setPositive(context.getString(R.string.dialog_wizard_target_locked_positive), null);
        commitFragment(context, dialogFragment, "targetLockedDialogTag");
        return dialogFragment;
    }

    public static DialogFragment targetCompletedDialog(Context context) {
        if (context == null || !SharedDataManager.showWizardOnTargetCompleted(context)) {
            return null;
        }
        if (targetCompletedDialog != null && targetCompletedDialog.isVisible()) {
            return targetCompletedDialog;
        }
        BasicInfoDialogFragment dialogFragment = new BasicInfoDialogFragment();
        dialogFragment.setText(
                context.getString(R.string.dialog_wizard_target_completed_title),
                context.getString(R.string.dialog_wizard_target_completed_message));
        dialogFragment.setPositive(context.getString(R.string.dialog_wizard_target_completed_positive), null);
        commitFragment(context, dialogFragment, "targetCompletedDialogTag");
        targetCompletedDialog = dialogFragment;
        return dialogFragment;
    }

    public static DialogFragment locationPermissionDialog(Context context) {
        if (context == null) {
            return null;
        }
        BasicInfoDialogFragment dialogFragment = new BasicInfoDialogFragment();
        dialogFragment.setText(
                context.getString(R.string.dialog_wizard_location_permission_title),
                context.getString(R.string.dialog_wizard_location_permission_message));
        dialogFragment.setPositive(context.getString(R.string.dialog_wizard_location_permission_positive), null);
        commitFragment(context, dialogFragment, "locationPermissionDialogTag");
        return dialogFragment;
    }

    public static DialogFragment getStandardProgressDialog(Context context, String title, String message) {
        if (context == null) {
            return null;
        }
        BasicProgressDialogFragment dialogFragment = new BasicProgressDialogFragment();
        dialogFragment.setTexts(title, message);
        dialogFragment.setCancelable(false);
        commitFragment(context, dialogFragment, "getStandardProgressDialogTag");
        return dialogFragment;
    }

    public static DialogFragment getServerCommunicationDialog(Context context) {
        if (context == null) {
            return null;
        }
        return getStandardProgressDialog(context, context.getString(R.string.server_communication),
                context.getString(R.string.waiting_for_data));
    }

    public static DialogFragment completeHistoryDialog(Context context) {
        if (context == null) {
            return null;
        }
        BasicInfoDialogFragment dialogFragment = new BasicInfoDialogFragment();
        dialogFragment.setText(
                context.getString(R.string.dialog_wizard_complete_history_title),
                context.getString(R.string.dialog_wizard_complete_history_message));
        dialogFragment.setPositive(context.getString(R.string.dialog_wizard_complete_history_positive), null);
        commitFragment(context, dialogFragment, "completeHistoryDialogTag");
        return dialogFragment;
    }

    public static void showPhotogenifiedNotification(Context context) {
        if (context == null) {
            return;
        }
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
        mBuilder.setContentIntent(resultPendingIntent).setAutoCancel(true);
        NotificationManagerCompat mNotificationManager = NotificationManagerCompat.from(context);
        mNotificationManager.notify(NOTIFICATION_PHOTOGENIFY_ID, mBuilder.build());
    }

    /**
     * Shows toast informing about the OHException with respect to its type.
     *
     * @param context Context of the caller.
     * @param exception OHException from server response.
     */
    public static void informOHException(Context context, OHException exception) {
        if (exception.getExType() == OHException.EXType.SERIALIZATION_INCOMPATIBLE) {
            Toast.makeText(context, context.getString(R.string.ohex_serialization),
                    Toast.LENGTH_SHORT).show();
        } else if (exception.getExType() == OHException.EXType.SERVER_OCCUPIED) {
            Toast.makeText(context, context.getString(R.string.ohex_occupied),
                    Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(context, context.getString(R.string.ohex_general) + " " + exception,
                    Toast.LENGTH_SHORT).show();
        }
        Log.e(context.getClass().getSimpleName(), context.getString(R.string.ohex_general) + exception);
    }

    /**
     * Shows toast informing about null response.
     *
     * @param context Context of the caller.
     */
    public static void informNullResponse(Context context) {
        Log.e(context.getClass().getSimpleName(), "Problem on client side");
        Toast.makeText(context, context.getString(R.string.server_unreachable_error),
                Toast.LENGTH_SHORT).show();
    }

}
