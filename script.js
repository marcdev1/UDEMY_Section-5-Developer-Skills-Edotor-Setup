// Remember, we're gonna use strict mode in all scripts now!
'use strict';

// console.log(
//   '======================================================================'
// );
// console.log(
//   '============== EP 59 Utilisation Stackoverflow, MDN =================='
// );
// console.log(
//   '======================================================================'
// );
// console.log('PROBLEM:');
// console.log(
//   'We work for a company building a smart hom thermometer. Our mlost recent task is: '
// );
// console.log(
//   '    Given an array of temperatures of one day, calculate the temperature amplitude. Keep in mind that sometimes there might be a sensor error.'
// );

// const temperatures = [3, -2, -6, -1, 'error', 9, 13, 17, 15, 14, 9, 5];

// console.log('1) Understanding the problem.');
// console.log(
//   '    - What is temp amplitutde? Answer: difference between highest and lowest temperature: '
// );
// console.log('    - How to compute max an min temperatures?');
// console.log("    - What's a sensor error? And what to do?");

// console.log('2) Breaking up into sub-problems.');
// console.log('    - How to ignore errors?');
// console.log('    - Find max value in temp array');
// console.log('    - Find min value in temp array');
// console.log('    - Substract min from max (amplitude)');

// const calcTempAmplitude = function (temps) {
//   let max = temps[0];
//   let min = temps[0];
//   for (let i = 0; i < temps.length; i++) {
//     const curTemp = temps[i];
//     if (typeof curTemp !== 'number') continue;
//     if (curTemp > max) max = curTemp;
//     if (curTemp < min) min = curTemp;
//   }
//   console.log(max, min);
//   return max - min;
// };
// const amplitude = calcTempAmplitude(temperatures);
// console.log(amplitude);

// // PROBLEM 2:
// // MERGE TWO ARRAYS

// // 1) Comprendre le problème
// // - Avec 2 tableaux, devons nous utiliser 2 fois la fonction? Non, fusionnez les deux tableaux

// // Breaking up into sub-problems
// // - Merge 2 ARRAYS
// const calcTempAmplitudeNew = function (t1, t2) {
//   const temps = t1.concat(t2);
//   console.log(temps);
//   let max = temps[0];
//   let min = temps[0];
//   for (let i = 0; i < temps.length; i++) {
//     const curTemp = temps[i];
//     if (typeof curTemp !== 'number') continue;
//     if (curTemp > max) max = curTemp;
//     if (curTemp < min) min = curTemp;
//   }
//   console.log(max, min);
//   return max - min;
// };
// const amplitudeNew = calcTempAmplitudeNew([3, 5, 1], [9, 0, 5]);
// console.log(amplitudeNew);
// console.log('');
// console.log('');
// console.log(
// '======================================================================';
//
// console.log(
//   '======================== EP 60 Débugger =============================='
// );
// console.log(
//   '======================================================================'
// );
console.log('');
const measureKelvin = function () {
  const measurement = {
    type: 'temp',
    unit: 'celsius',
    // Fix the bug => Attention la première lettre d'une fonction doit etre en majuscule
    // value: Number(prompt('Degrees celsius:')),
    value: 10,
    type2: 'temp2',
  };

  //Find the bug
  console.table(measurement);

  // console.log(measurement.value);
  // console.warn(measurement.value);
  // console.error(measurement.value);

  const kelvin = measurement.value + 273;
  return kelvin;
};
// Identify
console.log(measureKelvin());

//using a debugger
const calcTempAmplitudeBug = function (t1, t2) {
  const temps = t1.concat(t2);
  console.log(temps);
  let max = 0;
  let min = 0;
  for (let i = 0; i < temps.length; i++) {
    const curTemp = temps[i];
    if (typeof curTemp !== 'number') continue;

    if (curTemp > max) max = curTemp;
    if (curTemp < min) min = curTemp;
  }
  console.log(max, min);
  return max - min;
};
const amplitudeBug = calcTempAmplitudeBug([3, 5, 1], [9, 4, 5]);
// A) Identify
console.log(amplitudeBug);
