// Remember, we're gonna use strict mode in all scripts now!
'use strict';

console.log('PROBLEM:');
console.log(
  'We work for a company building a smart hom thermometer. Our mlost recent task is: '
);
console.log(
  '    Given an array of temperatures of one day, calculate the temperature amplitude. Keep in mind that sometimes there might be a sensor error.'
);

const temperatures = [3, -2, -6, -1, 'error', 9, 13, 17, 15, 14, 9, 5];

console.log('1) Understanding the problem.');
console.log(
  '    - What is temp amplitutde? Answer: difference between highest and lowest temperature: '
);
console.log('    - How to compute max an min temperatures?');
console.log("    - What's a sensor error? And what to do?");

console.log('2) Breaking up into sub-problems.');
console.log('    - How to ignore errors?');
console.log('    - Find max value in temp array');
console.log('    - Find min value in temp array');
console.log('    - Substract min from max (amplitude)');

const calcTempAmplitude = function (temps) {
  let max = temps[0];
  let min = temps[0];
  for (let i = 0; i < temps.length; i++) {
    const curTemp = temps[i];
    if (typeof curTemp !== 'number') continue;
    if (curTemp > max) max = curTemp;
    if (curTemp < min) min = curTemp;
  }
  console.log(max, min);
};
calcTempAmplitude([temperatures]);

//MERGE TWO ARRAYS
