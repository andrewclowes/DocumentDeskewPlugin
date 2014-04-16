/*
 * DocumentDeskew plugin for allowing communication between Cordova
 */
 
var argscheck = require('cordova/argscheck'),
    channel = require('cordova/channel'),
    utils = require('cordova/utils'),
    exec = require('cordova/exec');

/**
 * This represents the mobile device, and provides properties for inspecting the model, version, UUID of the
 * phone, etc.
 * @constructor
 */


var DocumentDeskew = {};

DocumentDeskew.open =  function(successCallback, errorCallback, scale) {
	var args = [scale];
    exec(successCallback, errorCallback, "DocumentDeskew", "open", args);
};

module.exports = DocumentDeskew;