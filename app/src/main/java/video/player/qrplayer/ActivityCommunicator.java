package video.player.qrplayer;

public class ActivityCommunicator {

    private static ActivityCommunicator activityCommunicator;

    public static ActivityCommunicator getCommunicator() {
        if(activityCommunicator == null) {
            activityCommunicator = new ActivityCommunicator();
        }
        return activityCommunicator;
    }

    public volatile Class returnActivity;
}
