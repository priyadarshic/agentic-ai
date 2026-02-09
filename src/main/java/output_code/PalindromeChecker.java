package output_code;

public class PalindromeChecker {

    /**
     * Checks if a given string is a palindrome.
     * A string is a palindrome if it reads the same forwards and backwards.
     * This method is case-insensitive and ignores non-alphanumeric characters.
     *
     * @param str The string to check.
     * @return true if the string is a palindrome, false otherwise.
     */
    public static boolean isPalindrome(String str) {
        if (str == null || str.isEmpty()) {
            return true; // An empty string or null is considered a palindrome
        }

        // Convert the string to lowercase and remove non-alphanumeric characters
        // to handle case-insensitivity and ignore spaces/punctuation
        String cleanedStr = str.toLowerCase().replaceAll("[^a-z0-9]", "");

        int left = 0;
        int right = cleanedStr.length() - 1;

        while (left < right) {
            if (cleanedStr.charAt(left) != cleanedStr.charAt(right)) {
                return false; // Characters do not match, not a palindrome
            }
            left++;
            right--;
        }

        return true; // All characters matched, it is a palindrome
    }

    public static void main(String[] args) {
        // Example Usage:
        System.out.println("'madam' is a palindrome: " + isPalindrome("madam")); // true
        System.out.println("'racecar' is a palindrome: " + isPalindrome("racecar")); // true
        System.out.println("'A man, a plan, a canal: Panama' is a palindrome: " + isPalindrome("A man, a plan, a canal: Panama")); // true
        System.out.println("'hello' is a palindrome: " + isPalindrome("hello")); // false
        System.out.println("'' is a palindrome: " + isPalindrome("")); // true
        System.out.println("null is a palindrome: " + isPalindrome(null)); // true
        System.out.println("'Was it a car or a cat I saw?' is a palindrome: " + isPalindrome("Was it a car or a cat I saw?")); // true
        System.out.println("'No lemon, no melon' is a palindrome: " + isPalindrome("No lemon, no melon")); // true
    }
}
