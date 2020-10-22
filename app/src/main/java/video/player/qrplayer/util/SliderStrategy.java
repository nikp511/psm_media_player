package video.player.qrplayer.util;

public interface SliderStrategy {

    int progressOf(final double value);


    double valueOf(final int progress);

    final class Quadratic implements SliderStrategy {
        private final double leftGap;
        private final double rightGap;
        private final double center;

        private final int centerProgress;


        public Quadratic(double minimum, double maximum, double center, int maxProgress) {
            if (center < minimum || center > maximum) {
                throw new IllegalArgumentException("Center must be in between minimum and maximum");
            }

            this.leftGap = minimum - center;
            this.rightGap = maximum - center;
            this.center = center;

            this.centerProgress = maxProgress / 2;
        }

        @Override
        public int progressOf(double value) {
            final double difference = value - center;
            final double root = difference >= 0 ?
                    Math.sqrt(difference / rightGap) :
                    -Math.sqrt(Math.abs(difference / leftGap));
            final double offset = Math.round(root * centerProgress);

            return (int) (centerProgress + offset);
        }

        @Override
        public double valueOf(int progress) {
            final int offset = progress - centerProgress;
            final double square = Math.pow(((double) offset) / ((double) centerProgress), 2);
            final double difference = square * (offset >= 0 ? rightGap : leftGap);

            return difference + center;
        }
    }
}
