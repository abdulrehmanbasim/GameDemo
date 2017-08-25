/*
 * Copyright (c) 2009, Balazs Lecz <leczbalazs@gmail.com>
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * 
 *     * Redistributions of source code must retain the above copyright notice,
 *       this list of conditions and the following disclaimer.
 * 
 *     * Redistributions in binary form must reproduce the above copyright notice,
 *       this list of conditions and the following disclaimer in the documentation
 *       and/or other materials provided with the distribution.
 * 
 *     * Neither the name of Balazs Lecz nor the names of
 *       contributors may be used to endorse or promote products derived from this
 *       software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 */

package com.medisense.android.tiltmazes;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import static android.content.Context.MODE_PRIVATE;


public class TiltMazesFragment extends Fragment implements ActivityListener{
    private static TiltMazesFragment instance =null;
    private static final String MAZE_GAME_TAG = "com.medisense.android.tiltmazes";
    private static final int MENU_RESTART = 1;
    private static final int MENU_MAP_PREV = 2;
    private static final int MENU_MAP_NEXT = 3;
    private static final int MENU_SENSOR = 4;
    private static final int MENU_SELECT_MAZE = 5;
    private static final int MENU_ABOUT = 6;
    private static final int REQUEST_SELECT_MAZE = 1;
    private static AlertDialog mGiniMazeSolvedDialog;
    private boolean mGameEndedSuccessfully = false;
    protected PowerManager.WakeLock mWakeLock;
    boolean cameFromMediApplication = false;
    int timeToPlayBeforeClosing = 1000 * 60 * 2;
    private MazeView mMazeView;
    private Dialog mAboutDialog;
    private Intent mSelectMazeIntent;
    private TextView mMazeNameLabel;
    private TextView mRemainingGoalsLabel;
    private TextView mStepsLabel;
    private GestureDetector mGestureDetector;
    private GameEngine mGameEngine;
    private Handler myHandler = new Handler();
    private boolean mActivityIsRunning = true;
    private Runnable closeControls = new Runnable() {
        @Override
        public void run() {
            if (mActivityIsRunning) {
                mGameEndedSuccessfully = true;
                mGiniMazeSolvedDialog = showCustomDialog(getActivity(), "", getResources().getString(R.string.end_of_game), getResources().getString(R.string.OK));
                mGiniMazeSolvedDialog.show();
                //broadCastGameEnd(mGameEndedSuccessfully);
            }

        }
    };

    public static TiltMazesFragment getInstance(){
        return instance = instance == null ? new TiltMazesFragment() : instance;
    }

    public static TiltMazesFragment instantiate(Bundle args)
    {
        instance = null;
       getInstance().setArguments(args);
        return instance;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.game_layout,null);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
//        getActivity().requestWindowFeature(Window.FEATURE_NO_TITLE);
//        getActivity().getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
//                WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
//        final PowerManager pm = (PowerManager) getActivity().getSystemService(Context.POWER_SERVICE);
//        mWakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "TiltMazes");

        mSelectMazeIntent = new Intent(getActivity(), SelectMazeActivity.class);

        // Build the About Dialog
        mAboutDialog = new Dialog(getActivity());
        mAboutDialog.setCancelable(true);
        mAboutDialog.setCanceledOnTouchOutside(true);
        mAboutDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        mAboutDialog.setContentView(R.layout.about_layout);

        Button aboutDialogOkButton = (Button) mAboutDialog.findViewById(R.id.about_ok_button);
        aboutDialogOkButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                mAboutDialog.cancel();
            }
        });



        //setContentView(R.layout.game_layout);

//        // Show the About Dialog on the first start
//        if (getActivity().getPreferences(MODE_PRIVATE).getBoolean("firststart", true)) {
//            getActivity().getPreferences(MODE_PRIVATE).edit().putBoolean("firststart", false).commit();
////			mAboutDialog.show(); Prevent dialog from showing on first time
//        }

        // Set up game engine and connect it with the relevant views
        mGameEngine = new GameEngine(getActivity());
        mMazeView = (MazeView) view.findViewById(R.id.maze_view);
        mGameEngine.setTiltMazesView(mMazeView);
        mMazeView.setGameEngine(mGameEngine);
        mMazeView.calculateUnit();

        mMazeNameLabel = (TextView) view.findViewById(R.id.maze_name);
        mGameEngine.setMazeNameLabel(mMazeNameLabel);
        mMazeNameLabel.setText(mGameEngine.getMap().getName());
        mMazeNameLabel.invalidate();

        mRemainingGoalsLabel = (TextView)view.findViewById(R.id.remaining_goals);
        mGameEngine.setRemainingGoalsLabel(mRemainingGoalsLabel);

        mStepsLabel = (TextView) view.findViewById(R.id.steps);
        mGameEngine.setStepsLabel(mStepsLabel);

//        mGameEngine.restoreState(
//                savedInstanceState,
//                getActivity().getPreferences(MODE_PRIVATE).getBoolean("sensorenabled", true)
//        );


        // Create gesture detector to detect flings
        mGestureDetector = new GestureDetector(new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2,
                                   float velocityX, float velocityY) {
                // Roll the ball in the direction of the fling
                Direction mCommandedRollDirection = Direction.NONE;

                if (Math.abs(velocityX) > Math.abs(velocityY)) {
                    if (velocityX < 0) mCommandedRollDirection = Direction.LEFT;
                    else mCommandedRollDirection = Direction.RIGHT;
                } else {
                    if (velocityY < 0) mCommandedRollDirection = Direction.UP;
                    else mCommandedRollDirection = Direction.DOWN;
                }

                if (mCommandedRollDirection != Direction.NONE) {
                    mGameEngine.rollBall(mCommandedRollDirection);
                }

                return true;
            }
        });
        mGestureDetector.setIsLongpressEnabled(false);

        //checkIfCameFromMedisense();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return mGestureDetector.onTouchEvent(event);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_LEFT:
                mGameEngine.rollBall(Direction.LEFT);
                return true;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                mGameEngine.rollBall(Direction.RIGHT);
                return true;
            case KeyEvent.KEYCODE_DPAD_UP:
                mGameEngine.rollBall(Direction.UP);
                return true;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                mGameEngine.rollBall(Direction.DOWN);
                return true;
            case KeyEvent.KEYCODE_BACK:
                Log.e("Mainactivity","OnBack Press "+ getActivity().getSupportFragmentManager().getBackStackEntryCount());
                if(getActivity().getSupportFragmentManager().getBackStackEntryCount()>0) {
                    getActivity().getSupportFragmentManager().popBackStack();
                }
                return true;
        }
        return false;
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        menu.add(0, MENU_MAP_PREV, 0, R.string.menu_map_prev);
        menu.add(0, MENU_RESTART, 0, R.string.menu_restart);
        menu.add(0, MENU_MAP_NEXT, 0, R.string.menu_map_next);
        menu.add(0, MENU_SENSOR, 0, R.string.menu_sensor);
        menu.add(0, MENU_SELECT_MAZE, 0, R.string.menu_select_maze);
        menu.add(0, MENU_ABOUT, 0, R.string.menu_about);

        menu.findItem(MENU_MAP_PREV).setIcon(getResources().getDrawable(android.R.drawable.ic_media_previous));
        menu.findItem(MENU_RESTART).setIcon(getResources().getDrawable(android.R.drawable.ic_menu_rotate));
        menu.findItem(MENU_MAP_NEXT).setIcon(getResources().getDrawable(android.R.drawable.ic_media_next));
        menu.findItem(MENU_SENSOR).setIcon(getResources().getDrawable(
                mGameEngine.isSensorEnabled() ? android.R.drawable.button_onoff_indicator_on : android.R.drawable.button_onoff_indicator_off
        ));
        menu.findItem(MENU_SELECT_MAZE).setIcon(getResources().getDrawable(android.R.drawable.ic_menu_more));
        menu.findItem(MENU_ABOUT).setIcon(getResources().getDrawable(android.R.drawable.ic_menu_info_details));

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_RESTART:
                mGameEngine.sendEmptyMessage(Messages.MSG_RESTART);
                return true;

            case MENU_MAP_PREV:
                mGameEngine.sendEmptyMessage(Messages.MSG_MAP_PREVIOUS);
                return true;

            case MENU_MAP_NEXT:

                mGameEngine.sendEmptyMessage(Messages.MSG_MAP_NEXT);
                return true;

            case MENU_SENSOR:
                mGameEngine.toggleSensorEnabled();
                item.setIcon(getResources().getDrawable(
                        mGameEngine.isSensorEnabled() ? android.R.drawable.button_onoff_indicator_on : android.R.drawable.button_onoff_indicator_off
                ));
                getActivity().getPreferences(MODE_PRIVATE).edit().putBoolean("sensorenabled", mGameEngine.isSensorEnabled()).commit();
                return true;

            case MENU_SELECT_MAZE:
                startActivityForResult(mSelectMazeIntent, REQUEST_SELECT_MAZE);
                return true;

            case MENU_ABOUT:
                mAboutDialog.show();
                return true;
        }

        return false;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case (REQUEST_SELECT_MAZE):
                if (resultCode == Activity.RESULT_OK) {
                    int selectedMaze = data.getIntExtra("selected_maze", 0);
                    mGameEngine.loadMap(0);
                }
                break;
        }
    }


    @Override
    public void onStop() {
        super.onStop();
        Log.d("mazeGame", "onStop");
    }

    @Override
    public void onPause() {
        super.onPause();
//        mGameEngine.unregisterListener();
//        mWakeLock.release();
//        Log.d("mazeGame", "onPause");
        getActivity().finish();
    }



    @Override
    public void onResume() {
        super.onResume();
//        mGameEngine.registerListener();
//        mWakeLock.acquire();
//        mGameEndedSuccessfully=false;
    }

    @Override
    public void onSaveInstanceState(Bundle icicle) {
        super.onSaveInstanceState(icicle);
//        mGameEngine.saveState(icicle);
//        mGameEngine.unregisterListener();
    }

//    @Override
//    protected void onRestoreInstanceState(Bundle savedInstanceState) {
//        super.onRestoreInstanceState(savedInstanceState);
//        mGameEngine.restoreState(
//                savedInstanceState,
//                getPreferences(MODE_PRIVATE).getBoolean("sensorenabled", true)
//        );
//    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("mazeGame", "onDestroy : mGameEndedSuccessfully : " + mGameEndedSuccessfully);
        instance = null;
        mActivityIsRunning = false;
        if (!mGameEndedSuccessfully&cameFromMediApplication) {
            //broadCastGameEnd(mGameEndedSuccessfully);
        }
    }

	public void onUserInteraction(){
		myHandler.removeCallbacks(closeControls);
        myHandler.postDelayed(closeControls, timeToPlayBeforeClosing);
	}

    private void broadCastGameEnd(boolean isEndedSuccessfully) {
        Log.e("broadCastGameEnd","isEndedSuccessfully :" + isEndedSuccessfully);
        Intent intent = new Intent();
        intent.setAction(MAZE_GAME_TAG);
        intent.putExtra(MAZE_GAME_TAG, isEndedSuccessfully);
        getActivity().sendBroadcast(intent);
        myHandler.removeCallbacks(closeControls);
    }

    public AlertDialog showCustomDialog(Context c, String headText, String messageText, String buttomText) {
        AlertDialog.Builder builder = new AlertDialog.Builder(c);
        LayoutInflater inflater = (LayoutInflater) c
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View dialog = inflater.inflate(R.layout.dialog, null);
        TextView message = (TextView) dialog.findViewById(R.id.dialog_message);
        TextView messageHead = (TextView) dialog
                .findViewById(R.id.dialog_message_head);
        TextView messageButtom = (TextView) dialog
                .findViewById(R.id.dialog_message_end);

        if (messageText != null && messageText.length() > 0) {
            message.setText(messageText);
        } else {
            message.setVisibility(View.GONE);
        }

        if (headText != null && headText.length() > 0) {
            messageHead.setText(headText);
//			if (headTextColorRes > 0)
//				messageHead.setTextColor(c.getResources().getColor(headTextColorRes));
        } else {
            messageHead.setVisibility(View.GONE);
        }

        if (buttomText != null && buttomText.length() > 0) {
            messageButtom.setText(buttomText);
        } else {
            messageButtom.setVisibility(View.GONE);
        }

        dialog.setOnClickListener(new View.OnClickListener() {


            public void onClick(View v) {
                mGiniMazeSolvedDialog.dismiss();
               // finish();
            }
        });

//        builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
//            public void onDismiss(DialogInterface dialog) {
//                getActivity().finish();
//            }
//        });

        dialog.findViewById(R.id.dialog_answer_container).setVisibility(
                View.GONE);

        builder.setView(dialog);
        return builder.create();

    }

    public void checkIfCameFromMedisense() {
        if (getArguments() != null && getArguments().getInt("time") > 0) {
            cameFromMediApplication = true;
            timeToPlayBeforeClosing = getArguments().getInt("time");
        }

        if (cameFromMediApplication) {
            myHandler.postDelayed(closeControls, timeToPlayBeforeClosing);
        }

        Log.e("MazeStarted", "came from medisense : " + cameFromMediApplication);
    }
}