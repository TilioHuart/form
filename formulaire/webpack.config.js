var path = require('path');

module.exports = {
    entry: {
        application: './formulaire/src/main/resources/public/ts/app.ts',
        behaviours: './formulaire/src/main/resources/public/ts/behaviours.ts'
    },
    output: {
        filename: '[name].js',
        path: __dirname + 'dest'
    },
    externals: {
        "entcore/entcore": "entcore",
        "entcore": "entcore",
        "moment": "entcore",
        "underscore": "entcore",
        "jquery": "entcore",
        "angular": "angular"
    },
    resolve: {
        modulesDirectories: ['node_modules'],
        root: path.resolve(__dirname),
        extensions: ['', '.ts', '.js'],
        alias: {
            "@common": path.resolve(__dirname, '../common/src/main/resources/ts'),
            "@formulairepublic": path.resolve(__dirname, '../formulaire-public/src/main/resources/public/ts')
        }
    },
    devtool: "source-map",
    module: {
        loaders: [
            {
                test: /\.ts$/,
                loader: 'ts-loader'
            }
        ]
    }
};
