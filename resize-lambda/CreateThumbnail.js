// dependencies
var async = require('async');
var AWS = require('aws-sdk');
var gm = require('gm')
    .subClass({ imageMagick: true }); // Enable ImageMagick integration.
var util = require('util');

// constants
var MAX_WIDTH  = 60;
var MAX_HEIGHT = 60;

// get reference to S3 client 
var s3 = new AWS.S3();

exports.handler = function(event, context) {
    // Read options from the event.
    console.log("Reading options from event:\n", util.inspect(event, {depth: 5}));
    var incomingBucket = event.Records[0].s3.bucket.name;
    // Object key may have spaces or unicode non-ASCII characters.
    var incomingKey    =
        decodeURIComponent(event.Records[0].s3.object.key.replace(/\+/g, " "));
    var processedBucket = incomingBucket.replace("incoming","processed");
    var rawBucket = incomingBucket.replace("incoming","raw");


    // Sanity check: validate that source and destination are different buckets.
    if (incomingBucket == processedBucket) {
        console.error("Destination bucket must not match source bucket.");
        return;
    }

    // Download the image from S3, transform, and upload to a different S3 bucket.
    async.waterfall([
            function download(next) {
                // Download the image from S3 into a buffer.
                s3.getObject({
                        Bucket: incomingBucket,
                        Key: incomingKey
                    },
                    next);
            },
            function tranform(response, next) {
                gm(response.Body).size(function(err, size) {
                    // Transform the image buffer in memory.
                    // Center, crop square, resize, strip metadata, convert to PNG
                    var smallestDimension = Math.min(size.width, size.height);
                    this.gravity('Center')
                        .extent(smallestDimension, smallestDimension)
                        .resize(MAX_WIDTH, MAX_HEIGHT)
                        .noProfile()
                        .toBuffer('PNG', function(err, buffer) {
                            if (err) {
                                next(err);
                            } else {
                                next(null, "image/png", buffer);
                            }
                        });
                });
            },
            function uploadProcessed(contentType, data, next) {
                // Stream the transformed image to the processed bucket.
                var params = {
                Bucket: processedBucket,
                Key: incomingKey,
                Body: data,
                ContentType: contentType
                };

                s3.putObject(params, function(err, data){
                if(err) console.log(err, err.stack);
                else console.log("Processed success logging"+data)
                    },
                    next(null));
            },
            function copyToRaw(next) {
                // Copy the incoming file to the raw bucket
                var params = {
                Bucket: rawBucket,
                Key: incomingKey,
                CopySource: incomingBucket + "/"  + incomingKey,
                MetadataDirective: "COPY"
                };

                s3.copyObject(params, function(err, data){
                if(err) console.log(err, err.stack);
                else console.log("Raw success logging "+data)
                    },
                    next);
            }
//            function cleanup(next) {
//                // Delete the file from the incoming bucket.
//               var params = {
//                   Bucket: incomingBucket,
//                   Key: incomingKey
//               };
//
//                s3.deleteObject(params, function(err,data){
//                if(err) console.log(err, err.stack);
//                else console.log("Delete incoming success logging "+data)
//                    },
//                    next);
//            }
        ], function (err) {
            if (err) {
                console.error(
                        'Unable to resize ' + incomingBucket + '/' + incomingKey +
                        ' and upload to ' + processedBucket + '/' + incomingKey +
                        ' due to an error: ' + err
                );
            } else {
                console.log(
                        'Successfully resized ' + incomingBucket + '/' + incomingKey +
                        ' and uploaded to ' + processedBucket + '/' + incomingKey
                );
            }

            context.done();
        }
    );
};
