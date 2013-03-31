ThreePaneLayout
===============

A three-pane layout where up to two panes are visible at a time.

When the left pane is showing, the middle pane will always be shown. When the
right pane is shown, it's up to the user to decide whether the middle pane is
shown.



Usage
=====

ThreePaneLayout can be used both in code and xml. It is not possible to
directly define child views in xml or add them by calling `ViewGroup#addView`,
but rather helper methods/attributes should be used.



XML definition
--------------

```xml
<net.simonvt.threepanelayout.ThreePaneLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:id="@+id/threePaneLayout"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    app:leftPaneLayout="@layout/left_pane"
    app:middlePaneLayout="@layout/middle_pane"
    app:rightPaneLayout="@layout/right_pane" />
```



Creating in code
----------------

```java
mThreePaneLayout = new ThreePaneLayout(context);
mThreePaneLayout.setLeftPaneLayout(R.layout.left_pane);
mThreePaneLayout.setMiddlePaneLayout(R.layout.middle_pane);
mThreePaneLayout.setRightPaneLayout(R.layout.right_pane);
```



License
=======

    Copyright 2013 Simon Vig Therkildsen

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
