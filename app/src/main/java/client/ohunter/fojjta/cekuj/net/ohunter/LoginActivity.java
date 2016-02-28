package client.ohunter.fojjta.cekuj.net.ohunter;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;

import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.kappa_labs.ohunter.lib.entities.Player;
import com.kappa_labs.ohunter.lib.net.OHException;
import com.kappa_labs.ohunter.lib.net.Response;
import com.kappa_labs.ohunter.lib.requests.LoginRequest;
import com.kappa_labs.ohunter.lib.requests.RegisterRequest;
import com.kappa_labs.ohunter.lib.requests.Request;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A login screen that offers login via email/password.
 */
public class LoginActivity extends AppCompatActivity implements Utils.OnResponseTaskCompleted /*implements LoaderCallbacks<Cursor>*/ {

    public static final String TAG = "LoginActivity";

//    /**
//     * Id to identity READ_CONTACTS permission request.
//     */
//    private static final int REQUEST_READ_CONTACTS = 0;
//
//    /**
//     * A dummy authentication store containing known user names and passwords.
//     * TODO: remove after connecting to a real authentication system.
//     */
//    private static final String[] DUMMY_CREDENTIALS = new String[]{
//            "foo@example.com:hello", "bar@example.com:world"
//    };
    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
//    private UserLoginTask mAuthTask = null;
    private Utils.RetrieveResponseTask mAuthTask = null;

    // UI references.
    private AutoCompleteTextView mNicknameAutoTextView;
    private AutoCompleteTextView mServerAutoTextView;
    private EditText mPasswordEditText;
    private View mProgressView;
    private View mLoginFormView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        // Set up the login form.
        mNicknameAutoTextView = (AutoCompleteTextView) findViewById(R.id.autoCompleteTextView_nickname);
//        populateAutoComplete();

        mServerAutoTextView = (AutoCompleteTextView) findViewById(R.id.autoCompleteTextView_server);
        // TODO: pridat pouzite/navrhovane adresy serveru
        mServerAutoTextView.setText("192.168.1.196:4242");

        mPasswordEditText = (EditText) findViewById(R.id.editText_password);
        mPasswordEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.login || id == EditorInfo.IME_NULL) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });

        Button mLogInButton = (Button) findViewById(R.id.button_login);
        mLogInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });


        Button mRegisterButton = (Button) findViewById(R.id.button_register);
        mRegisterButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptRegister();
            }
        });

        mLoginFormView = findViewById(R.id.scrollView_login_form);
        mProgressView = findViewById(R.id.progressBar_login);
    }

//    private void populateAutoComplete() {
//        if (!mayRequestContacts()) {
//            return;
//        }
//
//        getLoaderManager().initLoader(0, null, this);
//    }
//
//    private boolean mayRequestContacts() {
//        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
//            return true;
//        }
//        if (checkSelfPermission(READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
//            return true;
//        }
//        if (shouldShowRequestPermissionRationale(READ_CONTACTS)) {
//            Snackbar.make(mNicknameAutoTextView, R.string.permission_rationale, Snackbar.LENGTH_INDEFINITE)
//                    .setAction(android.R.string.ok, new View.OnClickListener() {
//                        @Override
//                        @TargetApi(Build.VERSION_CODES.M)
//                        public void onClick(View v) {
//                            requestPermissions(new String[]{READ_CONTACTS}, REQUEST_READ_CONTACTS);
//                        }
//                    });
//        } else {
//            requestPermissions(new String[]{READ_CONTACTS}, REQUEST_READ_CONTACTS);
//        }
//        return false;
//    }
//
//    /**
//     * Callback received when a permissions request has been completed.
//     */
//    @Override
//    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
//                                           @NonNull int[] grantResults) {
//        if (requestCode == REQUEST_READ_CONTACTS) {
//            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                populateAutoComplete();
//            }
//        }
//    }

    private View checkFields() {
        // Reset errors.
        mNicknameAutoTextView.setError(null);
        mPasswordEditText.setError(null);
        mServerAutoTextView.setError(null);

        // Store values at the time of the login attempt.
        String nickname = mNicknameAutoTextView.getText().toString();
        String password = mPasswordEditText.getText().toString();
        String server = mServerAutoTextView.getText().toString();

        View focusView = null;

        /* Check for a valid password, if the user entered one */
        if (!TextUtils.isEmpty(password) && !isPasswordValid(password)) {
            mPasswordEditText.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordEditText;
        }

        /* Check for a valid nickname */
        if (TextUtils.isEmpty(nickname)) {
            mNicknameAutoTextView.setError(getString(R.string.error_field_required));
            focusView = mNicknameAutoTextView;
        } else if (!isNicknameValid(nickname)) {
            mNicknameAutoTextView.setError(getString(R.string.error_invalid_nickname));
            focusView = mNicknameAutoTextView;
        }

        /* Check for a valid server address */
        if (TextUtils.isEmpty(server)) {
            mServerAutoTextView.setError(getString(R.string.error_field_required));
            focusView = mServerAutoTextView;
        } else if (!isServerValid(server)) {
            mServerAutoTextView.setError(getString(R.string.error_invalid_server));
            focusView = mServerAutoTextView;
        }

        return focusView;
    }

    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private void attemptLogin() {
        if (mAuthTask != null) {
            return;
        }

        View focusView = checkFields();
        if (focusView != null) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
//            showProgress(true);
//            mAuthTask = new UserLoginTask(email, password, server);
//            mAuthTask.execute((Void) null);

            /* Initialize the server address */
            String nickname = mNicknameAutoTextView.getText().toString();
            String password = mPasswordEditText.getText().toString();
            String server = mServerAutoTextView.getText().toString();
            String[] parts = server.split(":");
            String address = parts[0];
            int port = Integer.parseInt(parts[1]);
            Utils.initServer(address, port);

            /* Prepare the request */
            Request request = new LoginRequest(nickname, password);

            /* Asynchronously execute and wait for callback when result ready */
            mAuthTask = new Utils().new RetrieveResponseTask(this, Utils.getServerCommunicationDialog(this));
            mAuthTask.execute(request);
        }
    }

    private void attemptRegister() {
        if (mAuthTask != null) {
            return;
        }

        View focusView = checkFields();
        if (focusView != null) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
//            showProgress(true);

            /* Initialize the server address */
            String nickname = mNicknameAutoTextView.getText().toString();
            String password = mPasswordEditText.getText().toString();
            String server = mServerAutoTextView.getText().toString();
            String[] parts = server.split(":");
            String address = parts[0];
            int port = Integer.parseInt(parts[1]);
            Utils.initServer(address, port);

            /* Prepare the request */
            Request request = new RegisterRequest(nickname, password);

            /* Asynchronously execute and wait for callback when result ready */
            mAuthTask = new Utils().new RetrieveResponseTask(this, Utils.getServerCommunicationDialog(this));
            mAuthTask.execute(request);
        }
    }

    private boolean isNicknameValid(String nickname) {
        Pattern pattern = Pattern.compile("\\s");
        Matcher matcher = pattern.matcher(nickname);
        return !matcher.find();
    }

    private boolean isPasswordValid(String password) {
        //TODO: Replace this with your own logic
        return password.length() > 4;
    }

    private boolean isServerValid(String server) {
        String[] parts = server.split(":");
        if (parts.length != 2) {
            return false;
        }
        /* Check port part */
        try {
            Integer.parseInt(parts[1]);
        } catch (NumberFormatException ex) {
            return false;
        }
        /* Check IP address part */
        // NOTE: takto ne, network na main threadu nelze!
//        String address = parts[0];
//        try {
//            final InetAddress inet = InetAddress.getByName(address);
//            Log.d(TAG, "host: name = "+inet.getHostName()+", addr = "+inet.getHostAddress());
//            return true;
//        } catch (UnknownHostException e) {
//            return false;
//        }
        return true;
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            mLoginFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    @Override
    public void onResponseTaskCompleted(Response response, OHException ohex) {
        mAuthTask = null;

        /* Handle the error */
        if (ohex != null) {
//            Toast.makeText(LoginActivity.this, getString(R.string.recieved_ohex) + " " + ohex,
//                    Toast.LENGTH_SHORT).show();
            Log.e(TAG, getString(R.string.recieved_ohex) + ohex);
            switch (ohex.getExType()) {
                case INCORRECT_PASSWORD:
                    mPasswordEditText.setError(getString(R.string.error_incorrect_password));
                    mPasswordEditText.requestFocus();
                    break;
                case INCORRECT_USER:
                    mNicknameAutoTextView.setError(getString(R.string.error_incorrect_user));
                    mNicknameAutoTextView.requestFocus();
                    break;
                case DUPLICATE_USER:
                    mNicknameAutoTextView.setError(getString(R.string.error_duplicate_nickname));
                    mNicknameAutoTextView.requestFocus();
                    break;
                case DATABASE_ERROR:
                    Toast.makeText(LoginActivity.this, getString(R.string.error_database_problem),
                            Toast.LENGTH_SHORT).show();
                    break;
            }

//            showProgress(false);
            return;
        }

        /* Problem on client side */
        if (response == null) {
            Toast.makeText(LoginActivity.this, getString(R.string.server_unreachable_error),
                    Toast.LENGTH_SHORT).show();
            //NOTE: mozna pridat co by mel uzivatel zkusit: jina adresa serveru, chvili pockat...
//            showProgress(false);
            return;
        }

        /* Success */
        Player player = response.player;
        Log.d(TAG, "response login success: " + player);
        Intent intent = new Intent();
        intent.setData(null);
        MainActivity.mPlayer = player;
        setResult(RESULT_OK);
        finish();
    }

//    @Override
//    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
//        return new CursorLoader(this,
//                // Retrieve data rows for the device user's 'profile' contact.
//                Uri.withAppendedPath(ContactsContract.Profile.CONTENT_URI,
//                        ContactsContract.Contacts.Data.CONTENT_DIRECTORY), ProfileQuery.PROJECTION,
//
//                // Select only email addresses.
//                ContactsContract.Contacts.Data.MIMETYPE +
//                        " = ?", new String[]{ContactsContract.CommonDataKinds.Email
//                .CONTENT_ITEM_TYPE},
//
//                // Show primary email addresses first. Note that there won't be
//                // a primary email address if the user hasn't specified one.
//                ContactsContract.Contacts.Data.IS_PRIMARY + " DESC");
//    }
//
//    @Override
//    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
//        List<String> emails = new ArrayList<>();
//        cursor.moveToFirst();
//        while (!cursor.isAfterLast()) {
//            emails.add(cursor.getString(ProfileQuery.ADDRESS));
//            cursor.moveToNext();
//        }
//
//        addEmailsToAutoComplete(emails);
//    }
//
//    @Override
//    public void onLoaderReset(Loader<Cursor> cursorLoader) {
//
//    }
//
//    private void addEmailsToAutoComplete(List<String> emailAddressCollection) {
//        //Create adapter to tell the AutoCompleteTextView what to show in its dropdown list.
//        ArrayAdapter<String> adapter =
//                new ArrayAdapter<>(LoginActivity.this,
//                        android.R.layout.simple_dropdown_item_1line, emailAddressCollection);
//
//        mNicknameAutoTextView.setAdapter(adapter);
//    }
//
//
//    private interface ProfileQuery {
//        String[] PROJECTION = {
//                ContactsContract.CommonDataKinds.Email.ADDRESS,
//                ContactsContract.CommonDataKinds.Email.IS_PRIMARY,
//        };
//
//        int ADDRESS = 0;
//        int IS_PRIMARY = 1;
//    }
//
//    /**
//     * Represents an asynchronous login/registration task used to authenticate
//     * the user.
//     */
//    public class UserLoginTask extends AsyncTask<Void, Void, Boolean> {
//
//        private final String mEmail;
//        private final String mPassword;
//        private final String mServer;
//
//        UserLoginTask(String email, String password, String server) {
//            mEmail = email;
//            mPassword = password;
//            mServer = server;
//        }
//
//        @Override
//        protected Boolean doInBackground(Void... params) {
//            // TODO: attempt authentication against a network service.
//
//            try {
//                // Simulate network access.
//                Thread.sleep(2000);
//            } catch (InterruptedException e) {
//                return false;
//            }
//
//            for (String credential : DUMMY_CREDENTIALS) {
//                String[] pieces = credential.split(":");
//                if (pieces[0].equals(mEmail)) {
//                    // Account exists, return true if the password matches.
//                    return pieces[1].equals(mPassword);
//                }
//            }
//
//            // TODO: register the new account here.
//            return true;
//        }
//
//        @Override
//        protected void onPostExecute(final Boolean success) {
//            mAuthTask = null;
//            showProgress(false);
//
//            if (success) {
//                finish();
//            } else {
//                mPasswordEditText.setError(getString(R.string.error_incorrect_password));
//                mPasswordEditText.requestFocus();
//            }
//        }
//
//        @Override
//        protected void onCancelled() {
//            mAuthTask = null;
//            showProgress(false);
//        }
//    }
}

