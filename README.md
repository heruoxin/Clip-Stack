# Clip-Stack

## Deprecated

This project is deprecated and will not update anymore. 

### Why?

Clip Stack was the first work when I started learning Android. I chose open source for my past trust and gratitude to the open source community, but with its little fame, what happened next was unexpected.

There are endless open source thieves, simply change the package name, add advertising, put it on Play Store to make money. I reported too much, but it didn’t help.

Fortunately, with the addition of the app target API >= 26 requirement on Google Play, the source code which target 22 can no longer be easily compiled to publish to store.

Therefore, many open source robbers began to contact and attempt to coerce my synchronization code through emails, comments, and so on. After being pointed out and denounced by me, they actually swayed the content of seditiousness in some open source communities, and the people who did not know the truth came to get bad review.

### Status Quo

I realized that open source is not a good choice for applications that are intended for the average user rather than the programmer. For public-facing applications, open source attracts thieves and sprayers much more than people who contribute code or translators.

Compared to this source code, the version on Google Play upgraded the target API due to Google requirements, removed the floating ball and other features.

This app will not continue to update without significant compatibility issues (regardless of source code or Google Play).

## 此项目已停止更新

### 为什么？

剪纸堆是我初学 Android 时的第一个作品，出于既往的信任和对开源社区的感恩，我选择了开源，但随着它小有名气，随后发生的事情让人始料未及。

不断有无穷无尽的开源小偷，简简单单的改个包名，再加上广告，就放到 Play 上骗钱骗下载。举报到让人心力交瘁。

幸运的是，随着 Google Play 新增了应用 target API >= 26 的要求，这份 target 22 的源码不再能简简单单编译一下就上线骗钱了。

因此，很多开源强盗开始通过邮件、评论等方式联系并企图胁迫我同步代码。被我指出并痛斥后，他们竟然在某些开源社群布煽动性的内容，裹挟部分不明真相的群众前来差评。

### 现状

我意识到，对于直接面向普通用户而非程序员的应用程序来说，开源不是一个合适的选择。对于面向大众的应用程序，开源吸引来的小偷和喷子，比贡献代码或翻译的人多得多。

相比此源码，Google Play 上的版本由于 Google 要求升级了 target API，移除了悬浮球等功能，其他无变更。

如无重大兼容性问题，此 App 也不会继续更新（无论源码或 Google Play）。

### A tiny clipboard history manager app.

![screenshot](http://ww4.sinaimg.cn/large/66cab368gw1ep3ki6o4yzj21eq0h67am.jpg)

[![Get it on Google Play Store](https://developer.android.com/images/brand/en_generic_rgb_wo_60.png)](https://play.google.com/store/apps/details?id=com.catchingnow.tinyclipboardmanager)

Other Market:

[F-Droid](https://f-droid.org/repository/browse/?fdid=com.catchingnow.tinyclipboardmanager)
[CoolAPK](http://coolapk.com/apk/com.catchingnow.tinyclipboardmanager)

#### Unlimited Clips

📌 Clip Stack can remember all your clipboard history and recover text after reboot. 

#### Easy to Manage

📌 Easy to search, edit, and delete by a simple swipe gesture. You can also export history into a plain-text file.

#### Useful Notification

📌 It can show your clipboard history in a simple notification. Help you switch between clips and paste them easily when typing. Will only show when new text copied.

#### Easy to Share

📌 Any clips are shareable. You can easily share clips to many apps such as Email, SMS/MMS, Twitter, and more.

#### Material Design

📌 Full material design, not only color & icon.

#### Auto Clean Up

📌 Using Android 🍭Lollipop's new JobScheduler API, Clip Stack can automatic clean up it's catches and RAM when phone is charging. 

#### Other Features

✓ Gratis
✓ Free/Libre and Open-Source
✓ No-Ads

- Support Android 4.0 above and work better with Android 🍭Lollipop. 👍

#### Permission Usage

RECEIVE_BOOT_COMPLETED:  Start a background service to listen the system clipboard. It only cost 4.5M - 6M RAM. You can close it in Settings if you really don't want it.


WRITE_EXTERNAL_STORAGE and READ_EXTERNAL_STORAGE:  For export clipboard history. This app won't write any other files to your SD card.

-----

### Credits

* [nispok/Snackbar](https://github.com/nispok/snackbar)
* [brnunes/SwipeableRecyclerView](https://github.com/brnunes/SwipeableRecyclerView)
* [EatHeat/FloatingExample](https://github.com/EatHeat/FloatingExample)
* selio/icon

### Translate

* [Traditional Chinese: jacky030607](http://apk.tw/thread-645505-1-1.html)
* [Serbian: pejakm](https://github.com/heruoxin/Clip-Stack/pull/4)
* [French: RyDroid](https://github.com/heruoxin/Clip-Stack/pull/10)
* [Korean: 준모](https://twitter.com/cns_)
* [Japanese: 厨二病少女699](http://weibo.com/ikaemon)

###License

This application is comprised of two parts:

1. the Java code are licensed under the MIT license;
2. All rights of other parts, but not limited to the icons, images, and UI designs are reserved.


-----

#剪纸堆

###一个超轻量级剪贴板历史记录管理软件。


####无限保存剪贴板历史

📌 剪纸堆会自动保留您复制过的每一段文字。就算重启后也会自动恢复。

####易于管理

📌 无论添加、搜索、编辑还是全部清空，都非常容易。而轻轻滑动即可逐条删除。

####有用的扩展通知

📌 当你可能要输入文字的时候，你最近的6条剪贴板记录会悄悄出现在通知栏上。你能在其中自由切换和粘贴。当不需要时，轻滑即可消去。

####自由分享

📌 每一条剪贴板记录都能分享给其他的程序，诸如 Twitter、Gmail、 Evernote、微信、QQ……

####Material Design

📌 不仅图标和颜色，剪纸堆的每一个细节都遵循 Material design 设计标准。尽我可能的利用了 Android 🍭Lollipop 的新特性。

####自动清理

📌 当手机持续出于充电状态几分钟后，剪纸堆会悄悄自动清理自己的缓存数据，和内存占用，——这全归功于 Android 🍭Lollipop 的全新定时任务 API


####其他特性

✓  免费 ✓  开源 ✓  无广告

- 支持 4.0 以上的所有版本 Android 系统，与 Android 5.0🍭Lollipop 最为搭配。

####权限说明

本程序共使用 2 组权限：

RECEIVE_BOOT_COMPLETED： 开机时启动后台服务以记录剪贴板。据网友反馈服务仅占用 4.5M 到 6M 左右的运行内存。不影响电池续航。真的不想要的话，可以在设置里关闭。

WRITE_EXTERNAL_STORAGE 以及 READ_EXTERNAL_STORAGE： 仅在「导出历史记录」情况下会用到。除此之外不会在存储卡中写入任何文件。
