package utils;

/**
 * Created by itjamal on 12/3/2016.
 */
public class EditDistance {
    private String astr;
    private String bstr;
    private String astr_new;
    private String bstr_new;
    private int[][] arr;
    private int[][] weight_arr;
    private int[][] path_arr;


    public int findDistance(String str1, String str2) {
        astr = str1;
        bstr = str2;

        arr = calcEditDistance();
        dynamicProgram();
        return calcDistance();
    }

    private int[][] calcEditDistance() {
        int m = astr.length();
        int n = bstr.length();
        int[][] arr = new int[m + 1][n + 1];

        for (int i = 0; i <= m; i++) {
            arr[i][0] = i;
        }
        for (int i = 1; i <= n; i++) {
            arr[0][i] = i;
        }

        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                int diff = 0;
                if (astr.charAt(i - 1) != bstr.charAt(j - 1)) {
                    diff = 1;
                }
                arr[i][j] = Math.min(Math.min(arr[i - 1][j] + 1, arr[i][j - 1] + 1),
                        arr[i - 1][j - 1] + diff);
            }
        }
        return arr;
    }

    private void dynamicProgram() {
        int m = astr.length();
        int n = bstr.length();

        weight_arr = new int[m + 1][n + 1];
        path_arr = new int[m + 1][n + 1];

        for (int i = 1; i <= m; i++) {
            path_arr[i][0] = -1;
            weight_arr[i][0] = arr[i - 1][0] + 1;
        }
        for (int i = 1; i <= n; i++) {
            path_arr[0][i] = 1;
            weight_arr[0][i] = arr[0][i - 1] + 1;
        }

        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                int diff = 1;
                if (astr.charAt(i - 1) == bstr.charAt(j - 1)) {
                    diff = 0;
                }
                int w_left = arr[i - 1][j] + 1;
                int w_right = arr[i][j - 1] + 1;
                int w_middle = arr[i - 1][j - 1] + diff;

                int min = Math.min(Math.min(w_left, w_right), w_middle);

                if (w_middle == min) {
                    path_arr[i][j] = 0;
                } else if (w_left == min) {
                    path_arr[i][j] = -1;
                } else if (w_right == min) {
                    path_arr[i][j] = 1;
                }
                weight_arr[i][j] = w_left + w_middle + w_right + arr[i][j];
            }
        }
    }

    private int calcDistance() {
        int i = arr.length - 1;
        int j = arr[0].length - 1;
        int distance = 0;

        astr_new = "";
        bstr_new = "";

        while ((i >= 0) && (j >= 0)) {
            int val = path_arr[i][j];
            if (val == 0) { // to middle
                i--;
                j--;
                if (i >= 0) {
                    astr_new = String.valueOf(astr.charAt(i)) + astr_new;
                }
                if (j >= 0) {
                    bstr_new = String.valueOf(bstr.charAt(j)) + bstr_new;
                }
            } else if (val == -1) { // to left
                i--;
                astr_new = String.valueOf(astr.charAt(i)) + astr_new;
                bstr_new = "-" + bstr_new;
            } else if (val == 1) { // to right
                j--;
                bstr_new = String.valueOf(bstr.charAt(j)) + bstr_new;
                astr_new = "-" + astr_new;
            }
        }

        for (int x = 0; x < astr_new.length(); x++) {
            if (astr_new.charAt(x) != bstr_new.charAt(x)) {
                distance++;
            }
        }

        return distance;
    }

    public static void main(String[] args) {
        EditDistance ed = new EditDistance();
        String str1 = "TARU";
        String str2 = "TARIX";
        int dist = ed.findDistance(str1, str2);
        System.out.println("Distance between '" + str1 + "' and '" + str2 + "' is " + dist + " letters");

    }
}
