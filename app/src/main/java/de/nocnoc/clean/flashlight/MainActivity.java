package de.nocnoc.clean.flashlight;

/*
 This file is part of CleanFlashLight.

 CleanFlashLight is free software: you can redistribute it and/or modify
 it under the terms of the GNU Lesser General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 CleanFlashLight is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Lesser General Public License for more details.

 You should have received a copy of the GNU Lesser General Public License
 along with Foobar.  If not, see <http://www.gnu.org/licenses/>.

 Diese Datei ist Teil von CleanFlashLight.

 CleanFlashLight ist Freie Software: Sie koennen sie unter den Bedingungen
 der GNU Lesser General Public License, wie von der Free Software Foundation,
 Version 3 der Lizenz oder (nach Ihrer Wahl) jeder spaeteren
 veroeffentlichten Version, weiterverbreiten und/oder modifizieren.

 CleanFlashLight wird in der Hoffnung, dass es nuetzlich sein wird, aber
 OHNE JEDE GEWAEHRLEISTUNG, bereitgestellt; sogar ohne die implizite
 Gewaehrleistung der MARKTFAEHIGKEIT oder EIGNUNG FUER EINEN BESTIMMTEN ZWECK.
 Siehe die GNU Lesser General Public License fuer weitere Details.

 Sie sollten eine Kopie der GNU Lesser General Public License zusammen mit diesem
 Programm erhalten haben. Wenn nicht, siehe <http://www.gnu.org/licenses/>.
 */

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

/**
 * MainActivity manages the user interactions
 *
 * @author nocnoc
 */
public class MainActivity extends Activity {

    private SimpleFlashLight flashLight;

    /**
     * Reacts on button click
     */
    private final View.OnClickListener onClickToggleFlashlight = new View.OnClickListener() {
        public void onClick(View view) {
            flashLight.switchFlash();
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        Button flashLightToggle = ((Button) findViewById(R.id.toggleLightButton));

        if (checkHasFlash()) {
            flashLight = SimpleFlashLightImpl.getInstance((CameraManager) getSystemService(Context.CAMERA_SERVICE));
        }

        if (flashLight != null && flashLight.openCamera()) {
            flashLightToggle.setEnabled(true);
            flashLightToggle.setOnClickListener(onClickToggleFlashlight);
        } else {
            flashLightToggle.setEnabled(false);
            flashLightToggle.setOnClickListener(null);
            Toast.makeText(this, R.string.flash_device_not_available, Toast.LENGTH_SHORT).show();
        }

    }


    /**
     * Checks if flash feature is available
     *
     * @return true if this android device provides a camera flash
     */
    private boolean checkHasFlash() {
        return getApplicationContext().getPackageManager()
                .hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    /**
     * Assure that camera is closed before leaving the app.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        flashLight.closeCamera();
    }
}
