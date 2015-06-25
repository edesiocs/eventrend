# Introduction #

EvenTrend is a program to track arbitrary data over time.  In short, you can create categories of data, graph multiple items at one time, look at the historical trend, and compare it against goals.  It's intended to be able to add data incrementally as it occurs with a minimum of clicks or presses.

The first step in using EvenTrend to the create a category, for example, "weight." To create a category, click the "Menu" button from the main screen and select "Add Category."  After a category is created, it will be listed on the main screen.  Whenever there's an event that you want to record occurs, launch EvenTrend, set the value, and click "Add."

Categories can be edited by long-clicking on the category row, and can be grouped into lists.  To access other groups, fling the main screen to the left or right.

There are are a lot of fields that can be filled out when creating a category.  Click on each the text of each field when adding a category for help, or continue reading.

# Data Input Overview #

Once categories have been created, data collection can commence.  Just type a number in the text entry box, or click the +/- buttons until the desired value is displayed, and click "Add."  That's it -- the value is automatically added to the database with the current time and the trend icons updated.  If you accidentally add some data from the wrong category or with the incorrect value, just click "Undo" near the top of the screen.  Note that the "undo" history is only 1 deep -- if you mess up twice, you'll have to click the "Menu" button, choose "Edit Entries", and delete or modify the entry in the subsequent screens.

By default, the "Now" checkbox is selected, which displays the current time and and data input will contain this timestamp.  If you'd like to input data for another time -- either in the future or past -- select the "Set Date" and "Set Time" buttons, set the date and time accordingly, and then click "Add".  Note that when a date or time other than the current time is selected, the background of the clock turns red.  This is just a visual reminder to let you know that you're not entering data for the current time, but some other one.  Simply select the "Now" checkbox to return to the current time.

# Category Help #

Categories may be added, deleted, or modified at any time.  If a category is deleted, all of it's associated data is deleted as well.  The behavior of changing a particular aspect of a category, such as Aggregation, is covered in each section below.

## Group Name ##

This is an arbitrary name; categories with the same group will  appear in a list on the same page.  For example, you may want to have categories such as "weight" and "exercise" under the group of "health," and the categories of "driving time" and "gas used" under the group of "auto."  The currently displayed group can be changed by flinging to the left or right on the main input screen.

## Category Name ##

The name of the data to be tracked.

## Goal ##

If there is a goal, such as reaching a particular, enter it here.  This will allow the graph to display a goal line for the category, and is used by the trend icons displayed in the category list to give and indication as wether you are moving toward or away from your goal.  If there is no goal, leaving this set to 0 should be fine.

## Color ##

The color has no effect on the operation of application, but makes the list of categories easier to differentiate.  Each category will also be graphed in this color, and the category name displayed in the color.

## Aggregation ##

If you'd like to collapse multiple entries into one, select the period that you'd like to be the minimum duration.  For example, if you're only concerned about how many cups of coffee you consume in a day, set the aggregation to "Day."  Each day will only ever have one entry representing the total number (or average, see below) of cups of coffee consumed.  However, if you'd like to record this figure per-hour, choose "Hour."  Choosing the largest aggregation period you're concerned about will minimized the amount of data the application has to store, and make a variety of calculations faster, since there will be less work to perform later (such as when viewing a graph or calculating some other category based on this one.)

Note that once entries are aggregated, they cannot be separated.  The timestamp of the entry will be that of the first entry added in the period.  This field can be changed at any time, but only affects entries that are added the change -- i.e., if you select "Day", add some entries, and then change the category to "Hour", all the previous entries will still be aggregated into days, and the new entries moving forward will be aggregated into hours.

## Interpolation ##

This field controls how data between entries are interpolated, which affects data correlation, graphing, and series that are calculated based on other series.  If in doubt, choose "Linear" or "Cubic."

### Linear ###

Linear interpolation draws a line directly between points.

### Cubic ###

Cubic interpolation draws an 'S' shaped curve between two points.  This is a cubic spline, with the controls points set such that the slope of the path entering and exiting a point will be as close to 0 as possible while being monotonically increasing on the X-axis. Note that this currently affects the display of the line only; it does not affect interpolation of points for the purposes of correlation or calculation.  Linear interpolation will be used for correlation and calculation.

### StepEarly ###

StepEarly draws a continuous step function from one point to the next, where the interpolated data will rise or lower to the next datapoint immediately.  If a graph is descending, this will appear as a step in the form of the letter 'L'.

### StepLate ###

StepLate draws a continuous step function from one point to the next, where the interpolated data will retain the value of the previous datapoint until the next datapoint is encountered.

### StepMid ###

StepMid draws a continuous step function from one point to the next, where the interpolated data will rise or lower to the next point at the midpoint between data points.

## Calculated / Synthetic ##

Calculated, or Synthetic, categories are based on the inputs of one or more other series, such as adding two series together or referencing the previous timestamp or value of a series.  No data is stored in the database, but is calculated on the fly.  If this option is selected, it is not possible to set a **Default Value**, **Increment/Decrement**, or **Zero Empty Periods**, as all those fields are related to data input.  Since this category will be calculated on the fly, they would have no effect.  Selecting this will also enabled a button that allows creating the formula by which the series will be calculated.

## Formula ##

Please see **Advanced Usage**.

## Default Value ##

Once a category is added, it will appear in a list on the main screen. This is the value that will filled in by default, so if there's a particular value that you tend to enter more frequently than others, feel free to set it here.  Note that the application has been updated to retain the last set value as it's default value, so this really only affects the category prior to inputting the first datapoint.

## Increment/Decrement ##

When recording an entry under a category, the value can be entered directly via the keyboard.  However, there is also a set of plus and minus buttons near the text entry. The value in this box controls how much the value in the text entry box is changed with one click of either button.

## Value Type ##

The value type determines how multiple entries are aggregated, both in data input (where multiple entries within the same period listed below will be aggregated into a single point) and during graphing and calendar views, where value may be temporarily aggregated for the purpose of display only.

An example may prove to illustrate the difference most effectively:  suppose you are tracking the number of hours exercised per day.  In this case, if you worked out an hour in the morning and and hour in the evening, you'd want the total for the day to be 2 hours, do you'd choose "Sum."  However, if  you're tracking your weight, and you weighed yourself in the morning and evening, you'd want the two values to be averaged together, so you'd choose "Average."  Note that this setting only makes a difference if and aggregation level other than "None" is selected.

## Zero Empty Periods ##

Have EvenTrend automatically insert 0's at for each aggregation period that passes without any entries.  The addition will occur at the start of the period, and if any entries are added later on within the period, the 0 will be updated to reflect the new value.

An example may help to explain this better: say you're trying to quit smoking, and are recording the number of cigarettes consumed per day.  You'd probably want to set the Aggregation to "Day" (or "Hour", the choice is yours) and select this option.  For any days in which you didn't have any cigarettes, a 0 would be automatically inserted.

However, if you were tracking your weight, you probably wouldn't want to select this:  if you miss weighing yourself one day, you just want to ignore the period instead of recording a weight of 0.  Note that this option is only available if the type is "Sum" and the aggregation period is not "None."

# Graph Overview #

By default, the graph will display only the categories which were present on the input page.  However, any arbitrary set of graph can be displayed by pressing the "Menu" button and choosing "Filter."  Click the checkboxes beside each category to enable or disable the display of the category.

The y-axis of the graph contains markers at the top and bottom denoting the minimum and maximum values of the displayed categories, and the x-axis the dates and/or times, depending on the zoom level.  Long-press the graph to bring up the zoom buttons.  Note that each category is drawn independently, that is, each category is scaled to take up as much of the vertical screen space as possible.

Each point on the graph can be clicked on to bring up a dialog with details of the point.  This dialog also a button to gather statistics regarding the displayed portion of the category.

The graph may be scrolled left or by dragging, and the data may be temporarily aggregated into other period via the pop-up menu.

Data may be correlated via non-time-shifted linear correlation by pressing the "Menu" button and choosing "Correlate."  Note that this correlation is only calculated over the displayed data, not the entire set of data for the category.  Since data correlation is highly dependent on the source data, the ratings should only be taken and rough guidelines.

# Calendar Overview #

The "Data Range Info" is only calculated over all the displayed data.  The solid color
at the top of the box indicates the trend going toward or away from the goal compared to the previous day, and the color of the value in the box denotes whether the trend was going toward or away from the goal compared to the previous day.  Brighter colors indicate a larger change, dimmer colors a smaller change, and gray indicates that the value was essentially flat (as determined by the sensitivity preferences.)

# Backup and Restore #

In order to save or restore a EvenTrend database, select "Edit Entries" from the main menu, and then select "Export to Mail," "Export to File," or "Restore."  "Export to File" will store a comma separated value dump of the databases to the sdcard of your phone under the directory "eventrend".  "Restore" reads one of these CSV files from the same directory and will replace all the current data.  Note that imported and/or exporting can take some time if there is a lot of data.

# Advanced Usage #

## Settings: Trending Parameters ##

The trend of a series is calculated thusly:

`trend(n) = trend(n-1) + (smoothing) * (value(n) - trend(n-1))`

where `n` is the last `history` entries.  Thus, it is an approximation of an exponentially smoothed weighted average (some shortcuts are taken.)  The following parameters affect this calculation, as well as as the interpretation of it's results.

Note that changes to the next items will only manifest once a new data point has been added to a series, or if the "Recalc Trends" menu item is selected.

### History ###

The number of data points to take into account when calculating trends and other statistics.  The default value is 20, and with a default smoothing constant of 0.1, there is no reason to make it larger.  The larger the value, the more data must be read from the internal database, the more calculations have to be done, and the slower a variety of operations will become.

### Standard Deviation Sensitivity ###

On the main screen, trend icons are selected based on the difference of the current trend and the trend value at the previous data point.  These trend icons are currently set to be arrows that point directly to the right (flat trend), 15, 30, or 45 degrees up or down, depending on the magnitude of the change.  The threshold for determining the incline of arrow is the standard deviation multiplied by the sensitivity.

With a this value set to 1.0, this means that a change of greater than 1 standard deviation (over the last "History" datapoints) will result in a trend icon that points 45 degrees up or down, and an arrow that points slightly up or down if the change exceeds 1/2 a standard deviation, and 15 degrees up or down if the changed exceeds 1/4 a standard deviation.  If the value is 0.5, that means the arrow will be set to 45 degrees at 1/2 a standard deviation, and 30 degrees at 1/4 a standard deviation, and 15 degrees at 1/8.  The default is 0.5.

On the calendar views, this has a similar function related to the intensity of the green or red color of value and trend changes.  Default is 1.0, although many people may want to drop this to 0.5 in order to see more wiggle out of their trend indicators.

### Smoothing Constant ###

This is a value indicating the weight of previous data points to the current
one.  A value 1.0 indicates that a strict average of the last "History" points will be taken.  Anything less than 1.0 indicates that recent points have more influence over the trend than previous points, and the smaller the value, the more important recent data is.  Default is 0.1.

## Calculated / Synthetics Categories ##

The formula for a synthetics series may be constructed of binary operations only, that is, of the form `x operator y`, and must evaluate to a series of values.  For example, `series "water" + series "beers"` and `40 - series "work hours"` both evaluate to a sequence, while `4 + 5` does not.  A series must consist of the word `series` followed by whitespace and the name of the category to use in double quotes, as in the examples presented above.

There is no implicit grouping, so instead of attempting to calculate `x + y + z`, the following should be used: `(x + y) + z` or `x + (y + z)`.

The following operations are supported:  `+`, `-`, `*`, `\`, and `previous`.  Each of the first four operations work as expected:  for each data point in each category, the a data point is interpolated (based on the category's "Interpolation") at the timestamp of the data point, and the two values are added, subtracted, multiplied, or divided.

The last operator deserves special comment:  it returns the difference of each value or timestamp in the series with the one preceding it.  For example, if the series were called `weight`, and the values `150, 153, 147` had been entered over time, the equation `series "weight" previous value` would evaluate to `0, 3, -6`.  If each datum in the series was entered precisely 1 day apart, the equation `series "weight" previous value` would yield `0, 86400000, 86400000` - this is because the timestamp of each entry is measured in milliseconds.  Because of this, some helpful constants are provided:  `minute`, `hour`, `am/pm`, `day`, `week`, `month`, `quarter`, and `year`, which are
defined as the average number of milliseconds in each periods' time.  So, in order to calculate how many hours between weighings, the following may be used: `(series "weight" previous timestamp) / hour`

Complete list of operators:
  * `+`
  * `-`
  * `*`
  * `/`
  * `previous`

Complete list of operands:
  * `series "series name"`
  * constant:
    * `minute`
    * `hour`
    * `am/pm`
    * `day`
    * `week`
    * `month`
    * `quarter`
    * `year`
    * `value` (usable only with `previous`)
    * `timestamp` (usable only with `previous`)
  * literal:
    * integer: `-2`, `4`, ...
    * float: `1.67`, `2.5e-5`, ....

Grouping operator:
  * `()`