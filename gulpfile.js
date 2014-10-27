/*jslint node: true */
'use strict';

/*global require */
var gulp = require('gulp'),
    gutil = require('gulp-util'),
    debug = require('gulp-debug'),
    jslint = require('gulp-jslint'),
    jsonlint = require('gulp-jsonlint'),
    download = require('gulp-download'),
    unzip = require('gulp-unzip'),
    shell = require('gulp-shell'),
    bump = require('gulp-bump'),

    path = require('path'),
    fs = require('fs'),
    minimist = require('minimist'),

    // Keep in sync with xwalk-nfc-extension-src/build.xml.
    crosswalkVersion = "10.38.221.0",

    argv = minimist(process.argv.slice(2)),

    paths = {
        extension: 'xwalk-nfc-extension-src',
        tools: 'xwalk-nfc-extension-src/tools',
        xwalk: 'xwalk-nfc-extension-src/lib/crosswalk-' + crosswalkVersion,
        app: 'xwalk-nfc-app',
        js: [
            'gulpfile.js',
            'xwalk-nfc-app/js/main.js',
            'xwalk-nfc-extension-src/js/xwalk-nfc-extension.js'
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

gulp.task('ant', ['ivy', 'jslint', 'jsonlint'], function () {
    return gulp.src(paths.extension)
        .pipe(shell("ant", { cwd: paths.extension }));
});

gulp.task('make_apk', ['ant'], function () {
    /*jslint nomen: true */
    var topdir = __dirname;
    /*jslint nomen: false */

    return gulp.src(paths.xwalk)
        .pipe(shell("python make_apk.py --enable-remote-debugging --manifest=<%= app %>/manifest.json --extensions=<%= extension %>/xwalk-nfc-extension/ --package=org.crosswalkproject.nfc", {
            cwd: paths.xwalk,
            templateData: {
                app: path.join(topdir, paths.app),
                extension: path.join(topdir, paths.extension)
            }
        }));
});

gulp.task('install', ['make_apk'], function () {
    var config = require('./package.json');

    return gulp.src(paths.xwalk)
        .pipe(shell("adb install -r Nfc_<%= version %>_<%= arch %>.apk", {
            cwd: paths.xwalk,
            templateData: {
                version: config.version,
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

gulp.task('default', ['jsonlint', 'jslint', 'ivy', 'ant', 'make_apk']);
