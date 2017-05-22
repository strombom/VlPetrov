package ievents;

import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;


/**
 * Created by author.
 *
 * This class should do the following: compute weekly volatility distribution similar to the one computed in "A geographical
 * model for the daily and weekly seasonal volatility in the foreign exchange market", page 420, but it uses the
 * intrinsic event approach from ""Patterns in high-frequency FX data: Discovery of 12 empirical scaling laws" and
 * "Bridging the gap between physical and intrinsic time", equation 13. In other word, the computed volatility is based
 * on the sum of the overshoot variability variability of overshoots (OS).
 *
 * The whole process consists of the following parts:
 *  1) to define the input parameters of the analysis, such like size of threshold and the length of one bean (for example, 10 min);
 *  2) to find sum of the variability of overshoots for each bean of a week;
 *  3) to compute average value of the previous step
 *  4) to normalize the final value in such a way that the mean activity is equal to 1.0
 *  5) profit
 */
public class VolatilitySeasonality {

//    private static int N_DELTAS = 10; // how many thresholds we use in order to find the DCcountScalingLaw
//    private static float MIN_DELTA = 0.0001f; // the lowest threshold used fot the DCcountScalingLaw
//    private static float MAX_DELTA = 0.05f; // the biggest threshold used fot the DCcountScalingLaw
    private static final long MLS_WEAK = 604800000; // number of milliseconds in a week
    private double threshold; // size of the threshold used to find the number of DC and the variability of overshoots
    private long timeOfBean; // defines the length (in milliseconds) of one bean
//    private DCcountScalingLaw dCcountScalingLaw; // will be computed in order to find the best threshold for the chosen bean size
//    private ArrayList<Long> dcTimes; // here we hold times of all DC IEs.
//    private ArrayList<Double> osVariabilities; // all OSs variabilityes the DCs in dcPrices.
    private DcOS dCoS; // an instance of the DcOS class which is used to compute all interested parameters.
    private long nBeansInWeek; // how many beans we have in one week considering the chosen bean size.
//    private long referenceTime = 0; // reference time which is supposed to be used to find an Id of a bean when a DC IE occurs
    private double[] activityList; // here we will store activity data for every bean.
    private long timeFirstDC; // holds time of the first registered DC; will be used in the "computeTotalNumBeans"
    private long timeLastDC; // holds time of the last registered DC; will be used in the "computeTotalNumBeans"

    /**
     *
     * @param threshold is size of the threshold used to find the number of DC and the variability of overshoots
     * @param timeOfBean is length (in milliseconds) of one bean
     */
    public VolatilitySeasonality(double threshold, long timeOfBean){
        this.threshold = threshold;
        this.timeOfBean = timeOfBean;
//        dcTimes = new ArrayList<>();
        dCoS = new DcOS(threshold, threshold, 1, threshold, threshold);
        nBeansInWeek = MLS_WEAK / timeOfBean;
        activityList = new double[(int) nBeansInWeek];
    }

    /**
     * This method should be called for every new price. It checks whether the algo finds a new DC IE at the given
     * price or not and saves variability of overshoots to a preliminary array of data.
     * @param aPrice is every new price.
     */
    public void run(Price aPrice){
        int iEvent = dCoS.run(aPrice);
        if (iEvent == 1 || iEvent == -1){
            long dcTime = aPrice.getTime();
            if (timeFirstDC == 0){
                timeFirstDC = dcTime;
            }
            int beanId = findBeanId(dcTime);
            activityList[beanId] += dCoS.computeOSvariability();
            timeLastDC = dcTime;
        }
    }

    /**
     * This method compute the index of a bean in which the DC IE occurs.
     * @param dcTime is the time of the observed DC IE.
     * @return index (Id) ot the correspondent bean.
     */
    private int findBeanId(long dcTime){
        DateTime dcDateTime = new DateTime(dcTime);
        long mondayBeforeTime = dcDateTime.withDayOfWeek(DateTimeConstants.MONDAY).withTimeAtStartOfDay().getMillis();
        long millsFromMonday = dcTime - mondayBeforeTime - 1;
        if (millsFromMonday > MLS_WEAK){
            System.out.println("ATTENTION! Winter time");
            millsFromMonday -= 3600000;
        }
        return (int)(millsFromMonday / timeOfBean);
    }

    /**
     * The finish function averages, normalizes to 1.0 and returns the all values in the activityList
     * @return activity distribution array
     */
    public double[] finish(){
        averageActivity();
        normalizeActivity();
        return activityList;
    }

    /**
     *
     * @return the total number of beans between the first and the last times of the registered DC IEs.
     */
    private double computeTotalNumWeeks(){
        return (double)(timeLastDC - timeFirstDC) / MLS_WEAK;
    }


    /**
     * The function normalizes the activity distribution in order to have the mean value of it equal to 1.0
     */
    private void normalizeActivity(){
        double sumActivity = 0;
        for (double aActivity : activityList){
            sumActivity += aActivity;
        }
        double coef = activityList.length / sumActivity;
        for (int i = 0; i < activityList.length; i++){
            activityList[i] = activityList[i] * coef;
        }
    }

    /**
     * The function simply divides all values in the activityList by the total number of full periods (weeks) in order
     * to find the average activity of each bean.
     */
    private void averageActivity(){
        double totalNumWeeks = computeTotalNumWeeks();
        for (int i = 0; i < activityList.length; i++){
            activityList[i] /= totalNumWeeks;
        }
    }





}