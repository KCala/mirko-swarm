const path = require('path');
const HtmlWebpackPlugin = require('html-webpack-plugin');
const CleanWebpackPlugin = require('clean-webpack-plugin');
const webpack = require('webpack');
const FaviconsWebpackPlugin = require('favicons-webpack-plugin');

module.exports = {
    entry: {
        app: './src/index.js'
    },
    devtool: 'inline-source-map',
    devServer: {
        contentBase: './dist',
        host: '0.0.0.0'
    },
    plugins: [
        new CleanWebpackPlugin(['dist'], {verbose: false}),
        new HtmlWebpackPlugin({
            title: 'Output Management',
            template: './src/index.html'
        }),
        new FaviconsWebpackPlugin({
            // Your source logo
            logo: './src/assets/favicon.png',
            // The prefix for all image files (might be a folder or a name)
            prefix: '',
            // Emit all stats of the generated icons
            emitStats: false,
            // Generate a cache file with control hashes and
            // don't rebuild the favicons until those hashes change
            persistentCache: true,
            // Inject the html into the html-webpack-plugin
            inject: true,
            // favicon background color (see https://github.com/haydenbleasel/favicons#usage)
            background: '#2c2c2c',
            // favicon app title (see https://github.com/haydenbleasel/favicons#usage)
            title: 'Mirko swarm',

            // which icons should be generated (see https://github.com/haydenbleasel/favicons#usage)
            icons: {
                android: true,
                appleIcon: true,
                appleStartup: true,
                coast: false,
                favicons: true,
                firefox: true,
                opengraph: false,
                twitter: false,
                yandex: false,
                windows: false
            }
        })
    ],
    output: {
        filename: '[name].bundle.js',
        path: path.resolve(__dirname, 'dist')
    },
    module: {
        rules: [
            {
                test: /\.css$/,
                use: [
                    'style-loader',
                    'css-loader'
                ]
            },
            {
                test: /\.(png|svg|jpg|gif)$/,
                use: {
                    loader: 'file-loader',
                    options: {
                        name: '[path][name].[ext]',
                        context: './src/',
                    }
                }
            },
            {
                test: /\.tsx?$/,
                use: 'ts-loader',
                exclude: /node_modules/
            }
        ]
    },
    resolve: {
        extensions: ['.tsx', '.ts', '.js']
    }
}
