package org.firstinspires.ftc.teamcode.teamcode2017;

import com.qualcomm.hardware.bosch.BNO055IMU;
import com.qualcomm.hardware.bosch.JustLoggingAccelerationIntegrator;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.robotcore.util.ElapsedTime;
import com.qualcomm.robotcore.util.Range;

import org.firstinspires.ftc.robotcore.external.Func;
import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.robotcore.external.navigation.Acceleration;
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.AxesOrder;
import org.firstinspires.ftc.robotcore.external.navigation.AxesReference;
import org.firstinspires.ftc.robotcore.external.navigation.Orientation;
import org.firstinspires.ftc.teamcode.game.robot.PathSeg;
import org.firstinspires.ftc.teamcode.game.robot.StartPosition;
import org.firstinspires.ftc.teamcode.game.robot.TeamColor;

import java.util.Locale;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * This is NOT an opmode.
 *
 * This class can be used to define all the specific hardware for a single robot.
 */
public class Robot2017 {
    //Hardware Vars
    public DcMotor flMotor;
    public DcMotor frMotor;
    public DcMotor blMotor;
    public DcMotor brMotor;
    public Servo markerServo;
    public DcMotor liftMotor;
    public DcMotor negLiftMotor;
    public Servo pulleyHolder;
    public BNO055IMU imu;

    //Drive Trains
    public DriveTrain drive;
    public GyroDriveTrain gyrodrive;

    //Init variables
    public StartPosition startPosition;
    public boolean isHooked;

    //FTC Set-Up
    private HardwareMap hwMap;
    private Telemetry telemetry;
    private ElapsedTime time;

    /*
        ROBOT CLASS
     */
    public Robot2017() {

    }

    public Robot2017(boolean isHooked, StartPosition pos) {
        this.isHooked = true;
        this.startPosition = pos;
    }

    //FTC Setup methods
    public void setTelemetry(Telemetry t) {
        this.telemetry = t;
    }

    public void setTime(ElapsedTime time) {
        this.time = time;
    }


    /*
        INITIALIZATION - Initialize standard Hardware interfaces
    */
    public void init(HardwareMap hwMap) {
        initHardwareMap(hwMap);
        initDriveTrain();
        initIMU();
        initGyroDriveTrain();
        hwMap.logDevices();
    }

    public void initHardwareMap(HardwareMap hwMap) {
        this.hwMap = hwMap;

        flMotor = hwMap.dcMotor.get("flmotor");
        frMotor = hwMap.dcMotor.get("frmotor");
        blMotor = hwMap.dcMotor.get("blmotor");
        brMotor = hwMap.dcMotor.get("brmotor");
        liftMotor = hwMap.dcMotor.get("liftMotor");
        negLiftMotor = hwMap.dcMotor.get("negLiftMotor");
        markerServo = hwMap.servo.get("markerServo");
        pulleyHolder = hwMap.servo.get("pulleyHolder");
        imu = hwMap.get(BNO055IMU.class, "imu");
    }


    /*
        ACTION METHODS - Things to make reading/writing (particularly Autonomous) easier
     */
    public void setMarkerDown(){ markerServo.setPosition(.487f); }

    public void setMarkerUp(){ markerServo.setPosition(.637f); }

    public double getHeading(){ return imu.getAngularOrientation(AxesReference.INTRINSIC, AxesOrder.ZYX, AngleUnit.DEGREES).firstAngle; }


    /*
        IMU/ GYROSCOPE STUFF
    */
    Orientation angles;
    Acceleration gravity;

    public void initIMU(){
        BNO055IMU.Parameters parameters = new BNO055IMU.Parameters();
        parameters.angleUnit           = BNO055IMU.AngleUnit.DEGREES;
        parameters.accelUnit           = BNO055IMU.AccelUnit.METERS_PERSEC_PERSEC;
        parameters.calibrationDataFile = "BNO055IMUCalibration.json"; // see the calibration sample opmode
        parameters.loggingEnabled      = true;
        parameters.loggingTag          = "IMU";
        parameters.accelerationIntegrationAlgorithm = new JustLoggingAccelerationIntegrator();

        imu.initialize(parameters);
    }

    public void composeIMUTelemetry() {

        // At the beginning of each telemetry update, grab a bunch of data
        // from the IMU that we will then display in separate lines.
        telemetry.addAction(new Runnable() { @Override public void run()
        {
            // Acquiring the angles is relatively expensive; we don't want
            // to do that in each of the three items that need that info, as that's
            // three times the necessary expense.
            angles   = imu.getAngularOrientation(AxesReference.INTRINSIC, AxesOrder.ZYX, AngleUnit.DEGREES);
            gravity  = imu.getGravity();
        }
        });

        telemetry.addLine()
                .addData("status", new Func<String>() {
                    @Override public String value() {
                        return imu.getSystemStatus().toShortString();
                    }
                })
                .addData("calib", new Func<String>() {
                    @Override public String value() {
                        return imu.getCalibrationStatus().toString();
                    }
                });

        telemetry.addLine()
                .addData("heading", new Func<String>() {
                    @Override public String value() {
                        return formatAngle(angles.angleUnit, angles.firstAngle);
                    }
                })
                .addData("roll", new Func<String>() {
                    @Override public String value() {
                        return formatAngle(angles.angleUnit, angles.secondAngle);
                    }
                })
                .addData("pitch", new Func<String>() {
                    @Override public String value() {
                        return formatAngle(angles.angleUnit, angles.thirdAngle);
                    }
                });

        telemetry.addLine()
                .addData("grvty", new Func<String>() {
                    @Override public String value() {
                        return gravity.toString();
                    }
                })
                .addData("mag", new Func<String>() {
                    @Override public String value() {
                        return String.format(Locale.getDefault(), "%.3f",
                                Math.sqrt(gravity.xAccel*gravity.xAccel
                                        + gravity.yAccel*gravity.yAccel
                                        + gravity.zAccel*gravity.zAccel));
                    }
                });
    }

    String formatAngle(AngleUnit angleUnit, double angle) {
        return formatDegrees(AngleUnit.DEGREES.fromUnit(angleUnit, angle));
    }

    String formatDegrees(double degrees){
        return String.format(Locale.getDefault(), "%.1f", AngleUnit.DEGREES.normalize(degrees));
    }


    /*
        NORMAL DRIVE TRAIN - issues when both trains are together
    */
    public void initDriveTrain() {
        drive = new DriveTrain();
    }

    public class DriveTrain {
        static final double COUNTS_PER_MOTOR_REV = 1120;    // REV Motor Encoder
        //andymark is 1440 (this needs to be fact-checked)
        static final double DRIVE_GEAR_REDUCTION = 1.0;     // This is < 1.0 if geared UP
        static final double WHEEL_DIAMETER_INCHES = 4.0;     // For figuring circumference
        static final double COUNTS_PER_INCH = (COUNTS_PER_MOTOR_REV * DRIVE_GEAR_REDUCTION) /
                (WHEEL_DIAMETER_INCHES * Math.PI);
        static final double ROBOT_WIDTH = 14.75;
        static final double TURN_LENGTH = ROBOT_WIDTH * Math.PI / 4;
        DcMotor.Direction leftDefaultDir = DcMotor.Direction.FORWARD;
        DcMotor.Direction rightDefaultDir = DcMotor.Direction.REVERSE;


        Queue<PathSeg> paths = new LinkedBlockingQueue();

        public DriveTrain() {
            resetMotors();
            stop();
        }

        public void stop() {
            flMotor.setPower(0);
            frMotor.setPower(0);
            blMotor.setPower(0);
            brMotor.setPower(0);
            resetMotors();
        }

        public void resetMotors() {
            flMotor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
            frMotor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
            blMotor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
            brMotor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
            flMotor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
            frMotor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
            blMotor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
            brMotor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
            flMotor.setDirection(leftDefaultDir);
            frMotor.setDirection(rightDefaultDir);
            blMotor.setDirection(leftDefaultDir);
            brMotor.setDirection(rightDefaultDir);
        }

        public void vertical(double length) throws InterruptedException {
            PathSeg path = new PathSeg(-length, -length, -length, -length, time); //Hehe
            startPath(path);
            wait1((int) length / 12 * 500);
            wait1(1000);
        }

        public void horizontal(double length) throws InterruptedException { //Hehe
            PathSeg right = new PathSeg(-length, length, length, -length, time);
            startPath(right);
            wait1((int) length / 12 * 500);
            wait1(1000);
        }

        public void turn(int degree) throws InterruptedException { //Hehe
            PathSeg turn = new PathSeg(-2 * TURN_LENGTH * degree / 90, 2 * TURN_LENGTH * degree / 90, -2 * TURN_LENGTH * degree / 90, 2 * TURN_LENGTH * degree / 90, time);
            startPath(turn);
            wait1(Math.abs(degree * 10));
            wait1(1000);
        }

        public void turnRight() throws InterruptedException { //Hehe
            PathSeg left = new PathSeg(-2 * TURN_LENGTH, 2 * TURN_LENGTH, -2 * TURN_LENGTH, 2 * TURN_LENGTH, time);
            startPath(left);
            wait1(2000);

        }

        public void turnLeft() throws InterruptedException { //Hehe
            PathSeg right = new PathSeg(2 * TURN_LENGTH, -2 * TURN_LENGTH, 2 * TURN_LENGTH, -2 * TURN_LENGTH, time);
            startPath(right);
            wait1(2000);
        }
        private void startPath(PathSeg path) {

            // Turn On RUN_TO_POSITION
            flMotor.setMode(DcMotor.RunMode.RUN_TO_POSITION);
            frMotor.setMode(DcMotor.RunMode.RUN_TO_POSITION);
            blMotor.setMode(DcMotor.RunMode.RUN_TO_POSITION);
            brMotor.setMode(DcMotor.RunMode.RUN_TO_POSITION);

            // Determine new target position, and pass to motor controller
            path.flTarget = flMotor.getCurrentPosition() + (int) (path.fld * COUNTS_PER_INCH);
            path.frTarget = frMotor.getCurrentPosition() + (int) (path.frd * COUNTS_PER_INCH);
            path.blTarget = blMotor.getCurrentPosition() + (int) (path.bld * COUNTS_PER_INCH);
            path.brTarget = brMotor.getCurrentPosition() + (int) (path.brd * COUNTS_PER_INCH);

            flMotor.setTargetPosition(path.flTarget);
            frMotor.setTargetPosition(path.frTarget);
            blMotor.setTargetPosition(path.blTarget);
            brMotor.setTargetPosition(path.brTarget);

            flMotor.setPower(Math.abs(path.speed));
            frMotor.setPower(Math.abs(path.speed));
            blMotor.setPower(Math.abs(path.speed));
            brMotor.setPower(Math.abs(path.speed));
        }

        //return true if path done / conditions met, return false if still pathing
        public boolean pathDone() {
            PathSeg path = paths.peek();

            telemetry.addData("Path.clicks.final", "Running to %7d :%7d", path.flTarget, path.frTarget, path.blTarget, path.brTarget);
            telemetry.addData("Path.clicks.current", "Running at L%7d : R%7d", flMotor.getCurrentPosition(), frMotor.getCurrentPosition(), blMotor.getCurrentPosition(), brMotor.getCurrentPosition());
            telemetry.addData("path timed out", path.isTimedOut());
            telemetry.update();

            if (!path.isTimedOut()) {
                if (flMotor.isBusy()
                        && frMotor.isBusy()
                        && flMotor.getCurrentPosition() != path.flTarget
                        && frMotor.getCurrentPosition() != path.frTarget) {
                    return false;
                }
            }

            telemetry.addData("Path-", "finished");
            telemetry.update();
            return true;
        }

        public void queuePath(PathSeg path) {
            paths.add(path);
        }

        public void startPath() {
            startPath(paths.peek());
        }

        public void stopCurrPath() {
            removePath(paths.peek());
        }

        private void removePath(PathSeg path) {
            if (!paths.contains(path)) {
                return;
            }

            paths.remove(path);
            stop();
        }

        private void wait1(int t) throws InterruptedException {
            TimeUnit.MILLISECONDS.sleep(t);
        }
    }


    /*
        GYROSCOPE DRIVE TRAIN - issues when both trains are together
    */
    public void initGyroDriveTrain() { gyrodrive = new GyroDriveTrain(); }

    public class GyroDriveTrain{
        /* Declare OpMode members. */
        // INTERNAL IMU

        static final double     COUNTS_PER_MOTOR_REV    = 1440 ;    // eg: TETRIX Motor Encoder
        static final double     DRIVE_GEAR_REDUCTION    = 2.0 ;     // This is < 1.0 if geared UP
        static final double     WHEEL_DIAMETER_INCHES   = 4.0 ;     // For figuring circumference
        static final double     COUNTS_PER_INCH         = (COUNTS_PER_MOTOR_REV * DRIVE_GEAR_REDUCTION) /
                (WHEEL_DIAMETER_INCHES * 3.1415);

        // These constants define the desired driving/control characteristics
        // The can/should be tweaked to suite the specific robot drive train.
        static final double     DFLT_DRIVE_SPEED             = 0.7;     // Nominal speed for better accuracy.
        static final double     DFLT_TURN_SPEED              = 0.5;     // Nominal half speed for better accuracy.

        static final double     HEADING_THRESHOLD       = 1 ;      // As tight as we can make it with an integer gyro
        static final double     P_TURN_COEFF            = 0.5;     // Larger is more responsive, but also less stable
        static final double     P_DRIVE_COEFF           = 0.05;     // Larger is more responsive, but also less stable

        /**
         *  Method to drive on a fixed compass bearing (angle), based on encoder counts.
         *  Move will stop if either of these conditions occur:
         *  1) Move gets to the desired position
         *  2) Driver stops the opmode running.
         *
         * @param speed      Target speed for forward motion.  Should allow for _/- variance for adjusting heading
         * @param distance   Distance (in inches) to move from current position.  Negative distance means move backwards.
         * @param angle      Absolute Angle (in Degrees) relative to last gyro reset.
         *                   0 = fwd. +ve is CCW from fwd. -ve is CW from forward.
         *                   If a relative angle is required, add/subtract from current heading.
         */
        public void vertical ( double speed,
                                double distance,
                                double angle) throws InterruptedException {

            int     flTarget;
            int     frTarget;
            int     blTarget;
            int     brTarget;
            int     moveCounts;
            double  max;
            double  error;
            double  steer;
            double  leftSpeed;
            double  rightSpeed;

            // Determine new target position, and pass to motor controller
            flTarget = flMotor.getCurrentPosition() + (int) (-distance * COUNTS_PER_INCH);
            frTarget = frMotor.getCurrentPosition() + (int) (-distance * COUNTS_PER_INCH);
            blTarget = blMotor.getCurrentPosition() + (int) (-distance * COUNTS_PER_INCH);
            brTarget = brMotor.getCurrentPosition() + (int) (-distance * COUNTS_PER_INCH);

            // Set Target and Turn On RUN_TO_POSITION
            flMotor.setMode(DcMotor.RunMode.RUN_TO_POSITION);
            frMotor.setMode(DcMotor.RunMode.RUN_TO_POSITION);
            blMotor.setMode(DcMotor.RunMode.RUN_TO_POSITION);
            brMotor.setMode(DcMotor.RunMode.RUN_TO_POSITION);

            flMotor.setTargetPosition(flTarget);
            frMotor.setTargetPosition(frTarget);
            blMotor.setTargetPosition(blTarget);
            brMotor.setTargetPosition(brTarget);

            // Start motion.
            flMotor.setPower(Range.clip(Math.abs(speed), 0.0, 1.0)); // DONT KNOW WHY RANGE Math.abs(speed)
            frMotor.setPower(Range.clip(Math.abs(speed), 0.0, 1.0));
            blMotor.setPower(Range.clip(Math.abs(speed), 0.0, 1.0));
            brMotor.setPower(Range.clip(Math.abs(speed), 0.0, 1.0));

            // keep looping while we are still active, and BOTH motors are running.
            while (flMotor.isBusy() && frMotor.isBusy() && blMotor.isBusy() && brMotor.isBusy()) {

                // adjust relative speed based on heading error.
                error = getError(angle);
                steer = getSteer(error, P_DRIVE_COEFF);

                // if driving in reverse, the motor correction also needs to be reversed
                if (distance < 0)
                    steer *= -1.0;

                rightSpeed = speed - steer;
                leftSpeed = speed + steer;

                /*
                leftSpeed = speed - steer;
                rightSpeed = speed + steer;
                */

                // Normalize speeds if either one exceeds +/- 1.0;
                max = Math.max(Math.abs(leftSpeed), Math.abs(rightSpeed));
                if (max > 1.0)
                {
                    leftSpeed /= max;
                    rightSpeed /= max;
                }

                flMotor.setPower(leftSpeed);
                blMotor.setPower(leftSpeed);
                frMotor.setPower(rightSpeed);
                brMotor.setPower(rightSpeed);

                // Display drive status for the driver.
                telemetry.addData("Err/St",  "%5.1f/%5.1f",  error, steer);
                telemetry.addData("Target",  "%7d:%7d:%7d:%7d",      flTarget,  frTarget, blTarget,  brTarget);
                telemetry.addData("Actual",  "%7d:%7d",      flMotor.getCurrentPosition(),
                        frMotor.getCurrentPosition());
                telemetry.addData("Speed",   "%5.2f:%5.2f",  leftSpeed, rightSpeed);
                telemetry.update();
            }

            // Stop all motion;
            flMotor.setPower(0);
            frMotor.setPower(0);
            blMotor.setPower(0);
            brMotor.setPower(0);

            // Turn off RUN_TO_POSITION
            flMotor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
            frMotor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
            blMotor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
            brMotor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);


        }

        public void horizontal ( double speed,
                               double distance,
                               double angle) throws InterruptedException {

            int     flTarget;
            int     frTarget;
            int     blTarget;
            int     brTarget;
            int     moveCounts;
            double  max;
            double  error;
            double  steer;
            double backSpeed;
            double frontSpeed;

            // Determine new target position, and pass to motor controller
            flTarget = flMotor.getCurrentPosition() + (int) (-distance * COUNTS_PER_INCH);
            frTarget = frMotor.getCurrentPosition() - (int) (-distance * COUNTS_PER_INCH);
            blTarget = blMotor.getCurrentPosition() - (int) (-distance * COUNTS_PER_INCH);
            brTarget = brMotor.getCurrentPosition() + (int) (-distance * COUNTS_PER_INCH);

            // Set Target and Turn On RUN_TO_POSITION
            flMotor.setMode(DcMotor.RunMode.RUN_TO_POSITION);
            frMotor.setMode(DcMotor.RunMode.RUN_TO_POSITION);
            blMotor.setMode(DcMotor.RunMode.RUN_TO_POSITION);
            brMotor.setMode(DcMotor.RunMode.RUN_TO_POSITION);

            flMotor.setTargetPosition(flTarget);
            frMotor.setTargetPosition(frTarget);
            blMotor.setTargetPosition(blTarget);
            brMotor.setTargetPosition(brTarget);

            // start motion.
            flMotor.setPower(Range.clip(Math.abs(speed), 0.0, 1.0)); // DONT KNOW WHY RANGE Math.abs(speed)
            frMotor.setPower(Range.clip(Math.abs(speed), 0.0, 1.0));
            blMotor.setPower(Range.clip(Math.abs(speed), 0.0, 1.0));
            brMotor.setPower(Range.clip(Math.abs(speed), 0.0, 1.0));

            // keep looping while we are still active, and BOTH motors are running.
            while (flMotor.isBusy() && frMotor.isBusy() && blMotor.isBusy() && brMotor.isBusy()) {

                // adjust relative speed based on heading error.
                error = getError(angle);
                steer = getSteer(error, P_DRIVE_COEFF);

                // if driving in reverse, the motor correction also needs to be reversed
                if (distance < 0)
                    steer *= -1.0;


                backSpeed = speed + steer;
                frontSpeed = speed - steer;


                // Normalize speeds if either one exceeds +/- 1.0;
                max = Math.max(Math.abs(backSpeed), Math.abs(frontSpeed));
                if (max > 1.0)
                {
                    backSpeed /= max;
                    frontSpeed /= max;
                }

                flMotor.setPower(frontSpeed);
                blMotor.setPower(backSpeed);
                frMotor.setPower(frontSpeed);
                brMotor.setPower(backSpeed);

                // Display drive status for the driver.
                telemetry.addData("Err/St",  "%5.1f/%5.1f",  error, steer);
                telemetry.addData("Target",  "%7d:%7d:%7d:%7d",      flTarget,  frTarget, blTarget,  brTarget);
                telemetry.addData("Actual",  "%7d:%7d",      flMotor.getCurrentPosition(),
                        frMotor.getCurrentPosition());
                telemetry.addData("Speed",   "%5.2f:%5.2f",  frontSpeed, backSpeed);
                telemetry.update();
            }

            // Stop all motion;
            flMotor.setPower(0);
            frMotor.setPower(0);
            blMotor.setPower(0);
            brMotor.setPower(0);

            // Turn off RUN_TO_POSITION
            flMotor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
            frMotor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
            blMotor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
            brMotor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);


        }

        /*
            OLLLLLD TURN

        /**
         *  Method to spin on central axis to point in a new direction.
         *  Move will stop if either of these conditions occur:
         *  1) Move gets to the heading (angle)
         *  2) Driver stops the opmode running.
         *
         * @param speed Desired speed of turn.
         * @param angle      Absolute Angle (in Degrees) relative to last gyro reset.
         *                   0 = fwd. +ve is CCW from fwd. -ve is CW from forward.
         *                   If a relative angle is required, add/subtract from current heading.
         */
        /*
        public void turn (  double speed, double angle) throws InterruptedException {

            flMotor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
            frMotor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
            blMotor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
            brMotor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);

            // keep looping while we are still active, and not on heading.
            while (!onHeading(speed, angle, P_TURN_COEFF)) { //DELETED WHILE OPMODEACTIVE()
                // Update telemetry & Allow time for other processes to run.
                telemetry.update();
            }

            flMotor.setPower(0);
            frMotor.setPower(0);
            blMotor.setPower(0);
            brMotor.setPower(0);
            drive.wait1(500);
        }
        */

        /**
         *  Method to obtain & hold a heading for a finite amount of time
         *  Move will stop once the requested time has elapsed
         *
         * @param speed      Desired speed of turn.
         * @param angle      Absolute Angle (in Degrees) relative to last gyro reset.
         *                   0 = fwd. +ve is CCW from fwd. -ve is CW from forward.
         *                   If a relative angle is required, add/subtract from current heading.
         * @param holdTime   Length of time (in seconds) to hold the specified heading.
         */
        public void hold( double speed, double angle, double holdTime) throws InterruptedException {

            ElapsedTime holdTimer = new ElapsedTime();

            // keep looping while we have time remaining.
            holdTimer.reset();
            while (holdTimer.time() < holdTime) {
                // Update telemetry & Allow time for other processes to run.
                onHeading(speed, angle, P_TURN_COEFF);
                telemetry.update();
            }

            // Stop all motion;
            flMotor.setPower(0);
            frMotor.setPower(0);
            blMotor.setPower(0);
            brMotor.setPower(0);
        }

        /**
         * Perform one cycle of closed loop heading control.
         *
         * @param speed     Desired speed of turn.
         * @param angle     Absolute Angle (in Degrees) relative to last gyro reset.
         *                  0 = fwd. +ve is CCW from fwd. -ve is CW from forward.
         *                  If a relative angle is required, add/subtract from current heading.
         * @param PCoeff    Proportional Gain coefficient
         * @return
         */
        boolean onHeading(double speed, double angle, double PCoeff) throws InterruptedException {
            double   error ;
            double   steer ;
            boolean  onTarget = false ;
            double leftSpeed;
            double rightSpeed;

            // determine turn power based on +/- error
            error = getError(angle);

            if (Math.abs(error) <= HEADING_THRESHOLD) {
                steer = 0.0;
                leftSpeed  = 0.0;
                rightSpeed = 0.0;
                onTarget = true;
            }
            else {
                steer = getSteer(error, PCoeff);
                rightSpeed  = speed * steer;
                leftSpeed   = -rightSpeed;
            }

            // Send desired speeds to motors.

            flMotor.setPower(leftSpeed);
            blMotor.setPower(leftSpeed);
            frMotor.setPower(rightSpeed);
            brMotor.setPower(rightSpeed);


            // Display it for the driver.
            telemetry.addData("Target", "%5.2f", angle);
            telemetry.addData("Err/St", "%5.2f/%5.2f", error, steer);
            telemetry.addData("Speed.", "%5.2f:%5.2f", leftSpeed, rightSpeed);
            telemetry.addData("Actual Speed.", "%5.2f:%5.2f", flMotor.getPower(), frMotor.getPower());

            return onTarget;
        }

        /**
         * getError determines the error between the target angle and the robot's current heading
         * @param   targetAngle  Desired angle (relative to global reference established at last Gyro Reset).
         * @return  error angle: Degrees in the range +/- 180. Centered on the robot's frame of reference
         *          +ve error means the robot should turn LEFT (CCW) to reduce error.
         */
        public double getError(double targetAngle) throws InterruptedException {

            double robotError;

            // calculate error in -179 to +180 range  (
            robotError = targetAngle - imu.getAngularOrientation(AxesReference.INTRINSIC, AxesOrder.ZYX, AngleUnit.DEGREES).firstAngle;
            while (robotError > 180)  robotError -= 360;
            while (robotError <= -180) robotError += 360;

            if (Math.abs(robotError)== 180){
                robotError = 180;
            }
            return robotError;
        }

        /**
         * returns desired steering force.  +/- 1 range.  +ve = steer left
         * @param error   Error angle in robot relative degrees
         * @param PCoeff  Proportional Gain Coefficient
         * @return
         */
        public double getSteer(double error, double PCoeff) {
            return Range.clip(error * PCoeff, -1, 1);
        }

        private int correctCount = 0;
        /*
         * @param gyroTarget The target heading in degrees, between 0 and 360
         * @param gyroRange The acceptable range off target in degrees, usually 1 or 2
         * @param gyroActual The current heading in degrees, between 0 and 360
         * @param minSpeed The minimum power to apply in order to turn (e.g. 0.05 when moving or 0.15 when stopped)
         * @param addSpeed The maximum additional speed to apply in order to turn (proportional component), e.g. 0.3
         * @return The number of times in a row the heading has been in the range
         */
        public void turn(double maxSpeed, double gyroTarget){
            correctCount = 0;
            while (correctCount < 10) {
                turn(gyroTarget, 2.0, getHeading(), .05, maxSpeed-.05);
            }
        }

        public int turn(double gyroTarget, double gyroRange, double gyroActual, double minSpeed, double addSpeed) {
            double delta = (gyroTarget - gyroActual + 360.0) % 360.0; //the difference between target and actual mod 360
            if (delta > 180.0) delta -= 360.0; //makes delta between -180 and 180
            if (Math.abs(delta) > gyroRange) { //checks if delta is out of range
                this.correctCount = 0;
                double gyroMod = delta / 45.0; //scale from -1 to 1 if delta is less than 45 degrees
                if (Math.abs(gyroMod) > 1.0) gyroMod = Math.signum(gyroMod); //set gyromod to 1 or -1 if the error is more than 45 degrees
                this.newTurn(minSpeed * Math.signum(gyroMod) + addSpeed * gyroMod);
            }
            else {
                this.correctCount++;
                this.newTurn(0.0);
            }
            return this.correctCount;
        }

        public void newTurn(double sPower) {
            flMotor.setPower(+ sPower);
            blMotor.setPower(+ sPower);
            frMotor.setPower(- sPower);
            brMotor.setPower(- sPower);
        }
    }
}