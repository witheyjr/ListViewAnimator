ListViewAnimator
================

Allows very smoothly animated and versatile addition and removal of ListView items [mostly] regardless of layout complexity, unlike traditional SwipeDismiss implementations.

##Description

This code builds on AOSP code in order to animate both the removal and addition of ListView items smoothly and easily. Only implementing the ListViewAnimator interface and small tweaks to your adapter class are necessary in order to give your ListView smooth animations, regardless of layout complexity. This code was created because in my [News Alarm][news alarm] app, the traditional methods of resizing the views after swiping (via layoutParams) was jerky and unattractive on many devices. The [DevBytes][devbytes] AOSP code showed us how to make the removal animations very smooth, but did not take into account the addition of items.

- Uses an adapted form of SwipeToDismiss (Thanks [Roman Nurik][[roman-swipe-to-dismiss])
- Utilises the fantastic NineOldAndroids (Thanks [Jake Wharton][nineoldandroids])
- Also includes content from SwipeToDismissUndoList (Thanks [Tim Roes][timroes])

![screenshot][screenshot]

####[Sample apk][apk]



##Usage

Please download and see the sample app code for usage instructions and example.

##License

    Copyright (c) 2014 J Withey

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
    Come on, don't tell me you read that.
 
[screenshot]:https://raw.githubusercontent.com/chief-worminger/ListViewAnimator/master/screenshot.png
[apk]:https://github.com/chief-worminger/ListViewAnimator/blob/master/ListViewAnimator.apk
[roman-swipe-to-dismiss]:https://github.com/romannurik/Android-SwipeToDismiss
[news alarm]:https://play.google.com/store/apps/details?id=com.witheyjr.newsAlarm
[devbytes]:http://graphics-geek.blogspot.co.uk/2013/06/devbytes-animating-listview-deletion.html
[nineoldandroids]:http://nineoldandroids.com/
[timroes]:https://github.com/timroes/SwipeToDismissUndoList
