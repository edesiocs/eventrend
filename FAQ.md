# I don't get it.  What does this program do? #

This program provide a way to track events.  First, you have to tell it what you'd like to track, for example, number of times you floss in a week, how much you weigh, how many miles you drove, average number of times you cuss in a month, etc.  The interface is designed (hopefully) to minimize the number of clicks and presses required to record an event, so you don't end up spending your time recording data about your life, instead of living it.  This is why "Now" is selected by default -- the timestamp recorded with the event is set to "now".  (However, you can set the timestamp to an arbitrary date and time to record an event with a timestamp other than now.)

The steepest part of the learning curve is setting up events.  There are a lot of options when setting up a categories, but at the beginning, you only really need to worry about a couple parameters:

  * Aggregation
  * Type (Sum or Average)

Any parameter can be changed at any time, but the two mentioned above affect what happens when you record an event, so may be considered a little more important.  For details, see the [Documentation](http://code.google.com/p/eventrend/wiki/Documentation), or click on the parameter name to display a help screen, but in brief:

  * **Aggregation**  This determines if recording an event should modify on existing one, if it exists within the specified time frame (hour, day, week, etc), or create a new one.  If you'd like to counting cups of coffee consumed per day, adding "1.0" at 7:30 on Jan 1, 2009, and then again at 9:00, then the total for the day will be 2.0.  However, there's no way, later on, to change the aggregation to "hour" and examine the two separate events -- they are collapsed into one (to save space and make processing data quicker.)
  * **Type**  This determine what happens during the aggregation mentioned above.  Using the previous example, I omitted to mention that the counter will only be increased from 1.0 to 2.0 if the type is set to "Sum."  If they type is set to "Average", then the value would be 1.0.  Another way to look at the type is, if you want to measure a running total of something, use "Sum", but if you want to measure a rating, use "Average".

Each type of even is tracked in a "category", and categories can be organized into "groups."  All categories in a group are displayed in a single scrollable list.  To change groups, simply swipe your finger to the left or right, and the next group will appear.

# The interface sucks.  Why did you make that so crappy on an otherwise useful program? #

Thanks.  (Extra thanks if you gave me 1 star because you hated the interface.)  As a mentioned elsewhere, my goal for the user interface (at least, of the main input screen) to require minimal button presses to record data.  For my usage patterns, I think I accomplished this goal.  However, the way I use the program is not how everyone else will use the program, and considering I don't have my own usability lab at my disposal, I need your input to improve it!  [Email me](mailto:barclayosborn@gmail.com)!  [File feature requests](http://code.google.com/p/eventrend/issues/entry)!

# You said this could be used with the Hacker's Diet.  How do I set up my categories? #

You'll need 1-3 categories, depending on if you want to track weight, body fat percentage, and/or rungs.  Since I do exercises separate from the "exercise rungs" used within the diet, I'll omit a configuration for that here.  The current release of the software includes some sample categories along there lines, but for references, here's a sample configuration for average daily weight and body fat percentage, where omitted fields should be filled in with value appropriate to your situation:

| Group Name: | Hackers Diet |
|:------------|:-------------|
| Category Name: | Weight       |
| Aggregate By: | Day          |
| Interpolation: | Cubic        |
| Calculated: | (not selected) |
| Value Type: | Average      |
| Zero Empty Period: | (not selected) |

| Group Name: | Hackers Diet |
|:------------|:-------------|
| Category Name: | Body Fat %   |
| Aggregate By: | Day          |
| Interpolation: | Cubic        |
| Calculated: | (not selected) |
| Value Type: | Average      |
| Zero Empty Period: | (not selected) |

Feel free to set your goal, colors, as you prefer.  If you're weighing yourself in pounds or kilos, set "increment/decrement" to "1.0", or if you have a more accurate scale that measures 1/2 or 1/4 pounds (or kilos), you can set it to 0.5 or 0.25, respectively.  Any less than that probably wouldn't be accurate anyway, and will only make it so you have to press "+" or "-" more times to get to the value you want.

For a default value, I'd suggest entering something just a little below your current weight (if you're trying to gain weight, then something a little above your current weight.)  This will make it so there's a very small number of click required to enter a given weight, but gives you a little extra satisfaction when you lose enough that get to click the "-" button.

Now, whenever you add yourself, open the application, click the "+" or "-" button the get to the desired number (or use the keyboard), and click "Add."  With the categories configured as above, any weighings that occur on the same day will be averaged, and if you forget to weight yourself on a given day, the day will just be ignored.

Once you have some data entered, you can choose "Graph" or "Calendar View" from the menu on the main (category listing) screen, and see a graphical representation of your progress.