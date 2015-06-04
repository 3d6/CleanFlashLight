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

import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.util.Size;
import android.view.Surface;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.util.logging.Level.WARNING;

/**
 * SimpleFlashLightImpl provides
 * easy access and functionality to camera flashlight.
 * <p/>
 * It currently only supports lollipops new camera2 based functions
 * <p/>
 *
 * @author nocnoc
 */
public class SimpleFlashLightImpl implements SimpleFlashLight {


    private static final Logger logger = Logger.getLogger(SimpleFlashLightImpl.class.getSimpleName());

    private static SimpleFlashLight instance;

    /**
     * flash switch state mode indicator.
     * The indicator is needed, for there is no way to request on sdk or hardware
     */
    private boolean isFlashOn = false;

    /**
     * camera manager
     */
    private CameraManager cameraManager;

    /**
     * device id from the used camera
     */
    private String cameraID;

    /**
     * camera device, while camera is opened.
     * needed for closing actions
     */
    private CameraDevice cameraDevice;

    /**
     * single caption request builder.
     * stored here for interaction between capture request and capture session callback.
     */
    private CaptureRequest.Builder requestBuilder;

    /**
     * whole camera caption session.
     * stored for closing actions.
     */
    private CameraCaptureSession captureSession;

    /**
     * preventing the app from closing before the camera shut down
     */
    private final Semaphore cameraOpenCloseLock = new Semaphore(1);

    /**
     * Dummy Texture Buffer.
     * It is a field, for the ability to free after use.
     */
    private SurfaceTexture dummyTexture;

    /**
     * Whole Capture session (repeating single event)
     */
    private final CameraCaptureSession.StateCallback flashSessionCallback = new CameraCaptureSession.StateCallback() {

        @Override
        public void onConfigured(CameraCaptureSession session) {
            captureSession = session;
            try {
                captureSession.setRepeatingRequest(requestBuilder.build(), null, null);
            } catch (CameraAccessException e) {
                logger.log(Level.WARNING, "Failed to setup flashlight", e);
            }
        }

        @Override
        public void onConfigureFailed(CameraCaptureSession session) {
            logger.log(Level.WARNING, "Failed to setup flashlight");
        }
    };

    /**
     * Single capture event (one flash)
     */
    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {

            cameraDevice = camera;

            try {
                // Create new builder to manage all settings manually
                requestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);

                // set auto focus to auto as prerequisite for the flash
                requestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO);

                // set flash mode to torch
                // -> disabled by default. using switch button
                //requestBuilder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_TORCH);

                // add a dummy buffer
                dummyTexture = new SurfaceTexture(1);
                Size minSize = getSmallestTextureBuffer(cameraID);
                dummyTexture.setDefaultBufferSize(minSize.getWidth(), minSize.getHeight());
                List<Surface> targetList = new ArrayList<>();
                Surface dummySurface = new Surface(dummyTexture);
                targetList.add(dummySurface);
                requestBuilder.addTarget(dummySurface);

                // start session
                cameraDevice.createCaptureSession(targetList, flashSessionCallback, null);
            } catch (CameraAccessException e) {
                logger.log(WARNING, "Failed to use camera", e);
            }
        }

        @Override
        public void onClosed(CameraDevice camera) {
            dummyTexture.release();
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            // do nothing. closing device would lead into error at this point
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            // do nothing. closing device would lead into error at this point
        }
    };


    private SimpleFlashLightImpl(CameraManager cameraManager) throws CameraAccessException {

        if (cameraManager == null) {
            throw new IllegalArgumentException("cameraManager must not be null");
        }

        this.cameraManager = cameraManager;
        this.cameraID = getCameraId(cameraManager);

        if (this.cameraID == null) {
            throw new CameraAccessException(CameraAccessException.CAMERA_DISCONNECTED, "No suitable camera found");
        }
    }

    /**
     * Returns an instance of a SimpleFlashLight
     *
     * @param cameraManager The camera manager of this device. must not be null.
     * @return Instance of a SimpleFlashLight or null if no suitable camera could be found
     */
    public static SimpleFlashLight getInstance(CameraManager cameraManager) {
        if (instance != null) {
            return instance;
        } else {
            try {
                instance = new SimpleFlashLightImpl(cameraManager);
                return instance;
            } catch (CameraAccessException e) {
                logger.log(WARNING, "Error while finding suitable camera device", e);
                return null;
            }
        }
    }


    /**
     * Searches the camera devices for the back camera
     * and assures that it is one with a flashlight.
     *
     * @return the id of the back camera
     * @throws CameraAccessException
     */
    private String getCameraId(CameraManager cameraManager) throws CameraAccessException {
        String[] ids = cameraManager.getCameraIdList();
        for (String id : ids) {
            CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(id);
            boolean isFlashAvailable = cameraCharacteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
            boolean isBackCamera = CameraCharacteristics.LENS_FACING_BACK == cameraCharacteristics.get(CameraCharacteristics.LENS_FACING);

            if (isFlashAvailable && isBackCamera) {
                return id;
            }
        }
        return null;
    }


    public boolean isInitialized() {
        return cameraManager != null && cameraID != null;
    }


    public boolean openCamera() {
        if (!isInitialized()) {
            throw new IllegalStateException("Error: Missing initialization!");
        }

        boolean success = false;

        try {
            cameraOpenCloseLock.acquire();
            cameraManager.openCamera(cameraID, stateCallback, null);
            success = true;
        } catch (InterruptedException e) {
            logger.log(Level.WARNING, "Interrupted while opening camera");
            success = false;
        } catch (CameraAccessException e) {
            logger.log(Level.WARNING, "Failed to access camera");
            success = false;
        } finally {
            cameraOpenCloseLock.release();
        }

        return success;
    }


    public boolean closeCamera() {
        if (!isInitialized()) {
            throw new IllegalStateException("Error: Missing initialization!");
        }

        boolean success = false;

        try {
            cameraOpenCloseLock.acquire();
            if (captureSession != null) {
                captureSession.close();
                captureSession = null;
            }
            if (cameraDevice != null) {
                cameraDevice.close();
                cameraDevice = null;
            }

            success = true;
        } catch (InterruptedException e) {
            logger.log(Level.WARNING, "Interrupted while closing camera");
            success = false;
        } finally {
            cameraOpenCloseLock.release();
        }

        return success;
    }

    public boolean isDeviceOpened() {
        return requestBuilder != null && captureSession != null;
    }


    public void switchFlash() {
        if (isFlashOn()) {
            turnOffFlash();
        } else {
            turnOnFlash();
        }
    }


    public void turnOnFlash() {

        if (!isDeviceOpened()) {
            throw new IllegalStateException("Error: Device is not opened!");
        }

        if (!isFlashOn()) {
            try {
                //stopping old request (restarting is necessary to avoid memory issues)
                captureSession.stopRepeating();

                // start new request
                requestBuilder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_TORCH);
                captureSession.setRepeatingRequest(requestBuilder.build(), null, null);
                this.isFlashOn = true;

            } catch (CameraAccessException e) {
                logger.log(WARNING, "Failed to enable flash", e);
            }
        }

    }

    public void turnOffFlash() {
        if (!isDeviceOpened()) {
            throw new IllegalStateException("Error: Device is not opened!");
        }

        if (isFlashOn) {
            try {
                //stopping old request (restarting is necessary to avoid memory issues)
                captureSession.stopRepeating();

                // start new request
                requestBuilder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_OFF);
                captureSession.setRepeatingRequest(requestBuilder.build(), null, null);
                this.isFlashOn = false;

            } catch (CameraAccessException e) {
                logger.log(WARNING, "Failed to disable flash", e);
            }
        }
    }

    public boolean isFlashOn() {
        return isFlashOn;
    }


    /**
     * Evaluates the smallest texture buffer size, for we do not want to save it,
     * but only need the flashlight.
     *
     * @return size of the smallest needed texture buffer size
     */
    private Size getSmallestTextureBuffer(String cameraID) throws CameraAccessException {

        // check which available output sizes are compatible with SurfaceTexture
        Size[] outputSizes = cameraManager.getCameraCharacteristics(cameraID)
                .get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                .getOutputSizes(SurfaceTexture.class);

        // assure that there is any compatibility
        if (outputSizes == null || outputSizes.length == 0) {
            throw new IllegalStateException(
                    "Camera " + cameraID + "does not support any output size.");
        }

        // search for the smallest size to safe memory
        Size chosen = outputSizes[0];
        for (Size s : outputSizes) {
            if (chosen.getWidth() >= s.getWidth() && chosen.getHeight() >= s.getHeight()) {
                chosen = s;
            }
        }

        return chosen;
    }
}
