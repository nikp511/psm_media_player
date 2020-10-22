package video.player.qrplayer.videoplayer.player;


public class VideoMgr {

    public static VideoLayout FIRST_FLOOR;
    public static VideoLayout SECOND_FLOOR;

    public static VideoLayout getFirstFloor() {
        return FIRST_FLOOR;
    }

    public static void setFirstFloor(VideoLayout vidlayout) {
        FIRST_FLOOR = vidlayout;
    }

    public static VideoLayout getSecondFloor() {
        return SECOND_FLOOR;
    }

    public static void setSecondFloor(VideoLayout vidlayout) {
        SECOND_FLOOR = vidlayout;
    }

    public static VideoLayout getCurrentJzvd() {
        if (getSecondFloor() != null) {
            return getSecondFloor();
        }
        return getFirstFloor();
    }

    public static void completeAll() {
        if (SECOND_FLOOR != null) {
            SECOND_FLOOR.onCompletion();
            SECOND_FLOOR = null;
        }
        if (FIRST_FLOOR != null) {
            FIRST_FLOOR.onCompletion();
            FIRST_FLOOR = null;
        }
    }
}
