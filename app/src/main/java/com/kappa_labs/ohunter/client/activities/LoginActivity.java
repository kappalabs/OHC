package com.kappa_labs.ohunter.client.activities;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.kappa_labs.ohunter.client.R;
import com.kappa_labs.ohunter.client.utilities.SharedDataManager;
import com.kappa_labs.ohunter.client.utilities.Utils;
import com.kappa_labs.ohunter.lib.entities.Player;
import com.kappa_labs.ohunter.lib.net.OHException;
import com.kappa_labs.ohunter.lib.net.Response;
import com.kappa_labs.ohunter.lib.requests.LoginRequest;
import com.kappa_labs.ohunter.lib.requests.RegisterRequest;
import com.kappa_labs.ohunter.lib.requests.Request;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * A login screen that offers login & registration via nickname & password.
 */
public class LoginActivity extends AppCompatActivity implements Utils.OnResponseTaskCompleted {

    public static final String TAG = "LoginActivity";

    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    private Utils.RetrieveResponseTask mAuthTask = null;

    /* UI references */
    private AutoCompleteTextView mNicknameAutoTextView;
    private AutoCompleteTextView mServerAutoTextView;
    private EditText mPasswordEditText;
    private Set<String> serverHistory;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        /* Set up the login form */
        mNicknameAutoTextView = (AutoCompleteTextView) findViewById(R.id.autoCompleteTextView_nickname);

        mServerAutoTextView = (AutoCompleteTextView) findViewById(R.id.autoCompleteTextView_server);
        serverHistory = SharedDataManager.getServerHistory(this);
        populateServerAutoComplete();

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
        assert mLogInButton != null;
        mLogInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });

        Button mRegisterButton = (Button) findViewById(R.id.button_register);
        assert mRegisterButton != null;
        mRegisterButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptRegister();
            }
        });

        updateValuesFromPreferences();
    }

    @Override
    public void onBackPressed() {
        /* Disallow going back to previous activity */
    }

    private void updateValuesFromPreferences() {
        mNicknameAutoTextView.setText(SharedDataManager.getLastNickname(this));
        mServerAutoTextView.setText(SharedDataManager.getLastServer(this));
    }

    @Override
    protected void onStop() {
        super.onStop();

        savePreferences();
    }

    private View checkFields(String nickname, String password, String server) {
        /* Reset errors */
        mNicknameAutoTextView.setError(null);
        mPasswordEditText.setError(null);
        mServerAutoTextView.setError(null);

        View focusView = null;

        /* Check for a valid password, if the user entered one */
        if (TextUtils.isEmpty(password)) {
            mPasswordEditText.setError(getString(R.string.error_field_required));
            focusView = mPasswordEditText;
        } else if (!isPasswordValid(password)) {
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
        } else {
            /* Server looks OK, add it to the history list */
            addServerInput(mServerAutoTextView.getText().toString());
        }

        return focusView;
    }

    private void initServerAddress(String server) {
        String[] parts = server.split(":");
        String address = parts[0];
        int port = Integer.parseInt(parts[1]);
        Utils.initServer(address, port);
    }

    /**
     * Attempts to login the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private void attemptLogin() {
        if (mAuthTask != null) {
            return;
        }

        /* Store values at the time of the login attempt */
        String nickname = mNicknameAutoTextView.getText().toString();
        String password = mPasswordEditText.getText().toString();
        String server = mServerAutoTextView.getText().toString();

        View focusView = checkFields(nickname, password, server);
        if (focusView != null) {
            /* There was an error; don't attempt login and focus the last form field with an error. */
            focusView.requestFocus();
        } else {
            /* Initialize the server address */
            initServerAddress(server);

            /* Prepare the request */
            Request request = new LoginRequest(nickname, Utils.getDigest(password));

            /* Asynchronously execute and wait for callback when result ready */
            mAuthTask = Utils.getInstance().
                    new RetrieveResponseTask(this, Utils.getServerCommunicationDialog(this));
            mAuthTask.execute(request);
        }
    }

    /**
     * Attempts to register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private void attemptRegister() {
        if (mAuthTask != null) {
            return;
        }

        /* Store values at the time of the login attempt */
        String nickname = mNicknameAutoTextView.getText().toString();
        String password = mPasswordEditText.getText().toString();
        String server = mServerAutoTextView.getText().toString();

        View focusView = checkFields(nickname, password, server);
        if (focusView != null) {
            /* There was an error; don't attempt login and focus the last form field with an error. */
            focusView.requestFocus();
        } else {
            /* Initialize the server address */
            initServerAddress(server);

            /* Prepare the request */
            Request request = new RegisterRequest(nickname, Utils.getDigest(password));

            /* Asynchronously execute and wait for callback when result ready */
            mAuthTask = Utils.getInstance().
                    new RetrieveResponseTask(this, Utils.getServerCommunicationDialog(this));
            mAuthTask.execute(request);
        }
    }

    private boolean isNicknameValid(String nickname) {
        Pattern pattern = Pattern.compile("\\s");
        Matcher matcher = pattern.matcher(nickname);
        return !matcher.find();
    }

    private boolean isPasswordValid(String password) {
        return password.length() > 4;
    }

    private boolean isServerValid(String server) {
        String[] parts = server.split(":");
        if (parts.length != 2) {
            return false;
        }
        /* Check port part */
        return parts[1].trim().matches("^[0-9]*$");
    }

    @Override
    public void onResponseTaskCompleted(Request request, Response response, OHException ohex, Object _data) {
        mAuthTask = null;

        /* Handle the error */
        if (ohex != null) {
            Log.e(TAG, getString(R.string.ohex_general) + ohex);
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

            return;
        }

        /* Problem on client side */
        if (response == null) {
            Log.e(TAG, "Problem on client side -> cannot leave Login activity yet...");
            Toast.makeText(LoginActivity.this,
                    getString(R.string.server_unreachable_error), Toast.LENGTH_SHORT).show();
            return;
        }

        /* Success */
        Player player = response.player;
        Log.d(TAG, "response login success: " + player);
        /* Save and set the current player */
        SharedDataManager.setPlayer(this, player);
        /* Let possible caller know, that Player is now available */
        setResult(RESULT_OK);
        finish();
    }

    private void populateServerAutoComplete() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_list_item_1,
                serverHistory.toArray(new String[serverHistory.size()]));
        mServerAutoTextView.setAdapter(adapter);
    }

    private void addServerInput(String input) {
        if (!serverHistory.contains(input)) {
            serverHistory.add(input);
            populateServerAutoComplete();
        }
    }

    private void savePreferences() {
        SharedDataManager.setLastNickname(this, mNicknameAutoTextView.getText().toString());
        SharedDataManager.setLastServer(this, mServerAutoTextView.getText().toString());
        SharedDataManager.setServerHistory(this, serverHistory);
    }

}

