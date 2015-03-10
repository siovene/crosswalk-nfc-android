/*jslint node: true */
'use strict';

/*global require */
var gulp = require('gulp'),
    gutil = require('gulp-util'),
    debug = require('gulp-debug'),
    coffeelint = require('gulp-coffeelint'),
    jslint = require('gulp-jslint'),
    jsonlint = require('gulp-jsonlint'),
    coffee = require('gulp-coffee'),
    sourcemaps = require('gulp-sourcemaps'),
    download = require('gulp-download'),
    zip = require('gulp-zip'),
    unzip = require('gulp-unzip'),
    shell = require('gulp-shell'),
    bump = require('gulp-bump'),
    concat = require('gulp-concat'),

    path = require('path'),
    fs = require('fs'),
    minimist = require('minimist'),
    del = require('del'),

    // Keep in sync with xwalk-nfc-extension-src/build.xml.
    crosswalkVersion = "10.39.235.3",

    argv = minimist(process.argv.slice(2)),

    paths = {
        extension: 'xwalk-nfc-extension-src',
        tools: 'xwalk-nfc-extension-src/tools',
        xwalk: 'xwalk-nfc-extension-src/lib/crosswalk-' + crosswalkVersion,
        app: 'xwalk-nfc-christmas-tree',
        coffee: [
            'xwalk-nfc-extension-src/coffee/*.coffee'
        ],
        js: [
            'gulpfile.js',
            'xwalk-nfc-app/js/main.js',
            'xwalk-nfc-christmas-tree/js/app.js',
            'xwalk-nfc-christmas-tree/js/services.js',
            'xwalk-nfc-christmas-tree/js/controllers.js'
        ],
        json: [
            'package.json',
            'xwalk-nfc-app/manifest.json'
        ]
    },

    urls = {
        ivy: 'http://www.mirrorservice.org/sites/ftp.apache.org/ant/ivy/2.4.0-rc1/apache-ivy-2.4.0-rc1-bin.zip'
    };


gulp.task('jslint', function () {
    return gulp.src(paths.js).pipe(jslint());
});

gulp.task('jsonlint', function () {
    return gulp.src(paths.json)
        .pipe(jsonlint())
        .pipe(jsonlint.reporter());
});

gulp.task('coffeelint', function () {
    return gulp.src(paths.coffee)
        .pipe(coffeelint('coffeelint.json'))
        .pipe(coffeelint.reporter());
});

gulp.task('coffee', ['coffeelint'], function () {
    return gulp.src(paths.coffee)
        .pipe(sourcemaps.init())
        .pipe(coffee({ bare: true })).on('error', gutil.log)
        .pipe(sourcemaps.write('maps'))
        .pipe(gulp.dest(path.join(paths.extension, 'js')));
});

gulp.task('concat', ['coffee'], function () {
    var files = [
        'nfc',
        'encdec',
        'dataobject',
        'watch',
        'callbacks',
        'recorddata',
        'readevent',
        'messagetype',
        'adapter',
        'tnf',
        'rtd',
        'utils'],
        dest_dir = path.join(paths.extension, 'js'),
        dest_name = 'xwalk-nfc-extension.js',
        dest_file = path.join(dest_dir, dest_name);

    files = files.map(function (name) {
        return path.join('xwalk-nfc-extension-src', 'js', name) + '.js';
    });

    del.sync(dest_file);
    return gulp.src(files)
        .pipe(concat(dest_name))
        .pipe(gulp.dest(dest_dir));
});

gulp.task('ivy', function () {
    var zipName = urls.ivy.substring(urls.ivy.lastIndexOf('/') + 1),
        zipPath = path.join(paths.tools, zipName),
        dirPath = path.join(paths.tools, 'apache-ivy-2.4.0-rc1'),
        jarName = 'ivy-2.4.0-rc1.jar',
        jarPath = path.join(dirPath, jarName);

    /*jslint node: true, stupid: true */
    if (!fs.existsSync(zipPath)) {
        download(urls.ivy).pipe(gulp.dest(paths.tools));
    } else {
        gutil.log("Apache Ivy was already downloaded.");
    }

    if (!fs.existsSync(dirPath)) {
        gulp.src(zipPath)
            .pipe(unzip())
            .pipe(gulp.dest(paths.tools));
    } else {
        gutil.log("Apache Ivy was already extracted.");
    }

    if (!fs.existsSync(path.join(paths.tools, jarName))) {
        gulp.src(jarPath).
            pipe(gulp.dest(paths.tools));
    } else {
        gutil.log("Apache Ivy jar was already in place.");
    }
});

gulp.task('ant', function () {
    return gulp.src(paths.extension)
        .pipe(shell("ant", { cwd: paths.extension }));
});

gulp.task('make_apk', ['concat', 'ant'], function () {
    /*jslint nomen: true */
    var topdir = __dirname;
    /*jslint nomen: false */

    return gulp.src(paths.xwalk)
        .pipe(shell("python make_apk.py --enable-remote-debugging --manifest=<%= app %>/manifest.json --extensions=<%= extension %>/xwalk-nfc-extension/ --package=org.crosswalkproject.crosswalk_nfc_demo", {
            cwd: paths.xwalk,
            templateData: {
                app: path.join(topdir, paths.app),
                extension: path.join(topdir, paths.extension)
            }
        }));
});

gulp.task('install', ['jsonlint', 'jslint', 'make_apk'], function () {
    return gulp.src(paths.xwalk)
        .pipe(shell("adb install -r CrosswalkNfcDemo_<%= arch %>.apk", {
            cwd: paths.xwalk,
            templateData: {
                arch: argv.arch
            }
        }));
});

gulp.task('bump', ['jslint', 'jsonlint'], function () {
    var type = argv.type;

    if (type === undefined) {
        type = 'patch';
    }

    gulp.src('./package.json')
        .pipe(bump({type: type}))
        .pipe(gulp.dest('./'));

    gulp.src(path.join(paths.app, 'manifest.json'))
        .pipe(bump({type: type}))
        .pipe(gulp.dest(paths.app));
});

gulp.task('release', ['make_apk'], function () {
    var config = require('./package.json'),
        files = [
            path.join(paths.xwalk, "CrosswalkNfcDemo_arm.apk"),
            path.join(paths.xwalk, "CrosswalkNfcDemo_x86.apk"),
            path.join(paths.extension, "xwalk-nfc-extension", "xwalk-nfc-extension.jar"),
            path.join(paths.extension, "xwalk-nfc-extension", "xwalk-nfc-extension.js"),
            path.join(paths.extension, "xwalk-nfc-extension", "xwalk-nfc-extension.json")
        ];

    return gulp.src(files)
        .pipe(zip('xwalk-nfc-extension-' + config.version + '.zip'))
        .pipe(gulp.dest('releases'));
});

gulp.task('default', ['jsonlint', 'jslint', 'ivy', 'ant', 'make_apk']);
