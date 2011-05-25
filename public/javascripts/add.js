/*
 * jQuery.fn.textLimit( limit, callback );
 *
 * Add a limit to your textarea and inputfields.
 *
 * $('.element').textLimit( 100 );
 *
 * Version 1.0.0
 * www.labs.skengdon.com/textLimit
 * www.labs.skengdon.com/textLimit/js/textLimit.min.js
 */
;
(function($) {
	$.fn.clearTextLimit = function() {
		return this.each(function() {
			this.onkeydown = this.onkeyup = null;
		});
	};
	$.fn.textLimit = function(limit, callback) {
		if (typeof callback !== 'function')
			var callback = function() {
			};
		return this.each(function() {
			this.limit = limit;
			this.callback = callback;
			this.onkeydown = this.onkeyup = function() {
				this.value = this.value.substr(0, this.limit);
				this.reached = this.limit - this.value.length;
				this.reached = (this.reached == 0) ? true : false;
				return this.callback(this.value.length, this.limit,
						this.reached);
			}
		});
	};
})(jQuery);

// TODO Share these magic numbers with Admin.update method.
$(document).ready(
		function() {
			$('#question_description').textLimit(
					1000,
					function(length, limit, reached) {
						$('#charLeft_description').text(
								(1000 - length) + " carácteres libres");
					});

			$('#question_title').textLimit(
					140,
					function(length, limit, reached) {
						$('#charLeft_title').text(
								(140 - length) + " carácteres libres");
					});
		});