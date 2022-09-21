var gulp = require('gulp');
var webpack = require('webpack-stream');
var merge = require('merge2');
const replace = require('gulp-replace');
var clean = require('gulp-clean');
var argv = require('yargs').argv;
var fs = require('fs');

var apps = ['formulaire', 'formulaire-public'];

if (argv.targetModule) {
    console.log("using arg:", argv.targetModule);
    apps = [argv.targetModule];
}

gulp.task('drop-cache', () => {
    var streams = [];
    apps.forEach(function (app) {
        streams.push(gulp.src(['./' + app + '/src/main/resources/public/dist'], {read: false}).pipe(clean()))
        streams.push(gulp.src(['./' + app + '/build'], {read: false}).pipe(clean()))
    });
    return merge(streams);
});

gulp.task('copy-mdi-font', ['drop-cache'], () => {
    var streams = [];
    apps.forEach(function (app) {
        streams.push(gulp.src('./node_modules/@mdi/font/fonts/*')
            .pipe(gulp.dest('./' + app + '/src/main/resources/public/mdi')))
    });
    return merge(streams);
});

gulp.task('webpack', ['copy-mdi-font'], () => {
    var streams = [];
    apps.forEach(function (app) {
        streams.push(gulp.src('./' + app + '/src/main/resources/public/**/*.ts')
            .pipe(webpack(require('./' + app + '/webpack.config.js')))
            .on('error', function handleError() {
                this.emit('end'); // Recover from errors
            })
            .pipe(gulp.dest('./' + app + '/src/main/resources/public/dist')))
    });
    return merge(streams);
});

gulp.task('build', ['webpack'], () => {
    var streams = [];
    apps.forEach(function (app) {
        streams.push(gulp.src("./" + app + "/src/main/resources/view-src/**/*.html")
            .pipe(replace('@@VERSION', Date.now()))
            .pipe(gulp.dest("./" + app + "/src/main/resources/view")));
        streams.push(gulp.src("./" + app + "/src/main/resources/public/dist/behaviours.js")
            .pipe(gulp.dest("./" + app + "/src/main/resources/public/js")));
    });
    return merge(streams);
});