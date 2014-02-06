package com.richardlucasapps.netlive;


import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.text.Html;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import com.richardlucasapps.netlive.R;

public class MainActivity extends Activity {

    MyApplication app;
    SharedPreferences.Editor edit;

    SharedPreferences sharedPref;
//TODO externalize the strings in this activity, show some class*
    //*denotes pun

	@Override
    protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction()
        .replace(android.R.id.content, new SettingsFragment())
        .commit();

        app = new MyApplication();
        sharedPref = PreferenceManager.getDefaultSharedPreferences(app.getInstance());
        boolean disabled = sharedPref.getBoolean("pref_key_auto_start", false);

        if(!disabled){
        
        Intent intent = new Intent(this, MainService.class);
        startService(intent);
        }

		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
		
		boolean firstrun = getSharedPreferences("START_UP_PREFERENCE", MODE_PRIVATE).getBoolean("firstrun", true);


		 if (firstrun){
			 AlertDialog.Builder builder = new AlertDialog.Builder(this);
			 builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		               // User clicked OK button
		           }
		       });
			 builder.setMessage("Thank you for using my app.\n\nIf you have any questions, recommendations, or you run into" +
			 		" a problem with the app, please do not hesititate to contact me" +
			 		" by going to the \"Send Feedback\" option in the menu.  Enjoy!")
		       .setTitle("Welcome to NetLive");
			 AlertDialog dialog = builder.create();
			 
			 AlertDialog newFragment = dialog;
			 newFragment.show();

             //MyApplication.getInstance().clearApplicationData();//This will delete the data from the

             getSharedPreferences("START_UP_PREFERENCE", MODE_PRIVATE)
		        .edit()
		        .putBoolean("firstrun", false)
		        .commit();
			    
		 }

	}

	
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.action_settings_help:
                showHelpDialog();
                return true;
//            case R.id.action_settings_beer_me:
//            	showBeerMeDialog();
//            	return true;
            case R.id.action_settings_about:
                showAboutDialog();
                return true;
                
            case R.id. action_settings_send_feedback:
            	 Intent Email = new Intent(Intent.ACTION_SEND);
                 Email.setType("message/rfc822");
                 Email.putExtra(Intent.EXTRA_EMAIL, new String[] { "richardlucasapps@gmail.com" });
                 Email.putExtra(Intent.EXTRA_SUBJECT, "Feedback");
                 //Email.putExtra(Intent.EXTRA_TEXT, "Dear ...," + "");
                 startActivity(Intent.createChooser(Email, "Send Feedback:"));
                 return true;
            case android.R.id.home:
            	startActivity(new Intent(MainActivity.this, MainActivity.class)); 
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    
    }


	private void showAboutDialog() {
		 AlertDialog.Builder aboutBuilder = new AlertDialog.Builder(this);
		 TextView myMsg = new TextView(this);
		 SpannableString s = new SpannableString("NetLive v2.1 Beta\n\nrichardlucasapps.com");
		 Linkify.addLinks(s, Linkify.WEB_URLS);
		 myMsg.setText(s);
		 myMsg.setTextSize(15);
		 myMsg.setMovementMethod(LinkMovementMethod.getInstance());
		 myMsg.setGravity(Gravity.CENTER);
		 aboutBuilder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
	           public void onClick(DialogInterface dialog, int id) {
	               // User clicked OK button
	           }
	       });
		 aboutBuilder.setView(myMsg)
	       .setTitle("About");
		 AlertDialog dialog = aboutBuilder.create();
		 
		 AlertDialog newFragment = dialog;
		 newFragment.show();
		
		
	}


	private void showHelpDialog() {
		 AlertDialog.Builder builder = new AlertDialog.Builder(this);
		 //TextView myMsg = new TextView(this);
		String s = "Thank you for using my app.\n\nIf you have any questions, recommendations, or you run into" +
			 		" a problem with the app, please do not hesititate to contact me" +
			 		" by going to the \"Send Feedback\" option in the menu.";
		
		String overviewTitle = "Overview";
		String overviewContent = "NetLive allows you to monitor your internet data transfer rate while also showing the app that is using the most data at the " +
                "current moment. It runs both in the notification drawer or as a widget, so you always have access to your transfer rate information. It does all " +
                "of this without using even 1% of your battery.";

		String si =  "NetLive uses the official International System of Units (SI). " +
				"This means, for example, that a kilobit is considered 1000 bits, not 1024 bits.";
		
		LayoutInflater inflater= LayoutInflater.from(this);
		View view=inflater.inflate(R.layout.help_dialog, null);
		TextView textview = (TextView)view.findViewById(R.id.textmsg);
	    textview.setText((Html.fromHtml(s +"<br>"+"<br>" + "<b>" + overviewTitle + "</b>" + "<br>" + "<br>" + overviewContent + "<br>" + "<br>"
	    		+ si
	    		)));

	    textview.setTextSize(17);
	    textview.setPadding(15, 15, 15, 15);
		
		
		
		
		 builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
	           public void onClick(DialogInterface dialog, int id) {
	               // User clicked OK button
	           }
	       });
		 builder.setView(view)
	       .setTitle("Welcome to NetLive");
		 AlertDialog dialog = builder.create();
		 
		 AlertDialog newFragment = dialog;
		 newFragment.show();

		
	}
}
